package com.czertainly.ca.connector.ejbca.service.impl;

import com.czertainly.api.exception.NotFoundException;
import com.czertainly.api.model.common.attribute.v2.AttributeType;
import com.czertainly.api.model.common.attribute.v2.MetadataAttribute;
import com.czertainly.api.model.common.attribute.v2.content.AttributeContentType;
import com.czertainly.api.model.common.attribute.v2.content.StringAttributeContent;
import com.czertainly.api.model.common.attribute.v2.properties.MetadataAttributeProperties;
import com.czertainly.api.model.connector.v2.CertRevocationDto;
import com.czertainly.api.model.connector.v2.CertificateDataResponseDto;
import com.czertainly.api.model.connector.v2.CertificateRenewRequestDto;
import com.czertainly.api.model.connector.v2.CertificateSignRequestDto;
import com.czertainly.ca.connector.ejbca.api.AuthorityInstanceControllerImpl;
import com.czertainly.ca.connector.ejbca.api.CertificateControllerImpl;
import com.czertainly.ca.connector.ejbca.enums.UsernameGenMethod;
import com.czertainly.ca.connector.ejbca.service.CertificateEjbcaService;
import com.czertainly.ca.connector.ejbca.service.EjbcaService;
import com.czertainly.ca.connector.ejbca.util.CertificateUtil;
import com.czertainly.ca.connector.ejbca.util.CsrUtil;
import com.czertainly.core.util.AttributeDefinitionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CertificateEjbcaServiceImpl implements CertificateEjbcaService {

    public static final String META_EMAIL = "email";
    public static final String META_SAN = "san";
    public static final String META_EXTENSION = "extension";
    private static final Logger logger = LoggerFactory.getLogger(CertificateEjbcaServiceImpl.class);
    public final String META_EJBCA_USERNAME = "ejbcaUsername";
    private final String COMMON_NAME = "2.5.4.3";
    private EjbcaService ejbcaService;

    @Autowired
    public void setEjbcaService(EjbcaService ejbcaService) {
        this.ejbcaService = ejbcaService;
    }

    @Override
    public CertificateDataResponseDto issueCertificate(String uuid, CertificateSignRequestDto request) throws Exception {
        // generate username based on the request
        String usernameGenMethod = AttributeDefinitionUtils.getSingleItemAttributeContentValue(AuthorityInstanceControllerImpl.ATTRIBUTE_USERNAME_GEN_METHOD, request.getRaProfileAttributes(), StringAttributeContent.class).getData();
        String usernamePrefix = AttributeDefinitionUtils.getSingleItemAttributeContentValue(AuthorityInstanceControllerImpl.ATTRIBUTE_USERNAME_PREFIX, request.getRaProfileAttributes(), StringAttributeContent.class).getData();
        String usernamePostfix = AttributeDefinitionUtils.getSingleItemAttributeContentValue(AuthorityInstanceControllerImpl.ATTRIBUTE_USERNAME_POSTFIX, request.getRaProfileAttributes(), StringAttributeContent.class).getData();

        JcaPKCS10CertificationRequest csr = parseCsrToJcaObject(request.getPkcs10());

        String username = generateUsername(usernameGenMethod, usernamePrefix, usernamePostfix, csr);
        String subjectDn = csr.getSubject().toString();
        String password = username;

        // try to create end entity and issue certificate
        ejbcaService.createEndEntity(uuid, username, password, subjectDn, request.getRaProfileAttributes(), request.getAttributes());
        // issue certificate
        CertificateDataResponseDto certificate = ejbcaService.issueCertificate(uuid, username, password, Base64.getEncoder().encodeToString(csr.getEncoded()));

        certificate.setMeta(getIssueMetadata(
                username,
                AttributeDefinitionUtils.getSingleItemAttributeContentValue(CertificateControllerImpl.ATTRIBUTE_EMAIL, request.getRaProfileAttributes(), StringAttributeContent.class).getData(),
                AttributeDefinitionUtils.getSingleItemAttributeContentValue(CertificateControllerImpl.ATTRIBUTE_SAN, request.getRaProfileAttributes(), StringAttributeContent.class).getData(),
                AttributeDefinitionUtils.getSingleItemAttributeContentValue(CertificateControllerImpl.ATTRIBUTE_EXTENSION, request.getRaProfileAttributes(), StringAttributeContent.class).getData()
        ));

        return certificate;
    }

    @Override
    public CertificateDataResponseDto renewCertificate(String uuid, CertificateRenewRequestDto request) throws Exception {
        JcaPKCS10CertificationRequest csr = parseCsrToJcaObject(request.getPkcs10());

        List<MetadataAttribute> metadata = request.getMeta();

        // check if we have the username in the metadata, and if not, generate username
        String username = null;
        if (!request.getMeta().isEmpty()) {
            username = AttributeDefinitionUtils.getSingleItemAttributeContentValue(META_EJBCA_USERNAME, metadata, StringAttributeContent.class).getData();
        }
        if (StringUtils.isBlank(username)) {
            String usernameGenMethod = AttributeDefinitionUtils.getSingleItemAttributeContentValue(AuthorityInstanceControllerImpl.ATTRIBUTE_USERNAME_GEN_METHOD, request.getRaProfileAttributes(), StringAttributeContent.class).getData();
            String usernamePrefix = AttributeDefinitionUtils.getSingleItemAttributeContentValue(AuthorityInstanceControllerImpl.ATTRIBUTE_USERNAME_PREFIX, request.getRaProfileAttributes(), StringAttributeContent.class).getData();
            String usernamePostfix = AttributeDefinitionUtils.getSingleItemAttributeContentValue(AuthorityInstanceControllerImpl.ATTRIBUTE_USERNAME_POSTFIX, request.getRaProfileAttributes(), StringAttributeContent.class).getData();
            username = generateUsername(usernameGenMethod, usernamePrefix, usernamePostfix, csr);
        }

        String password = username;

        try {
            // update end entity
            ejbcaService.renewEndEntity(uuid, username, password);
        } catch (NotFoundException e) {
            String subjectDn = csr.getSubject().toString();
            ejbcaService.createEndEntityWithMeta(uuid, username, password, subjectDn, request.getRaProfileAttributes(), metadata);
        }

        // issue certificate
        CertificateDataResponseDto certificate = ejbcaService.issueCertificate(uuid, username, password, Base64.getEncoder().encodeToString(csr.getEncoded()));

        List<MetadataAttribute> meta = new ArrayList<>();
        meta.addAll(metadata.stream().filter(e -> !e.getName().equals(META_EJBCA_USERNAME)).collect(Collectors.toList()));
        meta.addAll(getUsernameMetadata(username));
        certificate.setMeta(meta);

        return certificate;
    }

    private List<MetadataAttribute> getUsernameMetadata(String username) {
        List<MetadataAttribute> attributes = new ArrayList<>();

        // Username
        MetadataAttribute attribute = new MetadataAttribute();
        attribute.setUuid("b42ab690-60fd-11ed-9b6a-0242ac120002");
        attribute.setName(META_EJBCA_USERNAME);
        attribute.setDescription("EJBCA Username");
        attribute.setType(AttributeType.META);
        attribute.setContentType(AttributeContentType.STRING);
        attribute.setContent(List.of(new StringAttributeContent(username)));

        MetadataAttributeProperties attributeProperties = new MetadataAttributeProperties();
        attributeProperties.setVisible(true);
        attributeProperties.setLabel("EJBCA Username");
        attribute.setProperties(attributeProperties);

        attributes.add(attribute);
        return attributes;
    }

    private List<MetadataAttribute> getIssueMetadata(String username, String email, String san, String extensions) {
        List<MetadataAttribute> attributes = new ArrayList<>();

        // Username
        attributes.addAll(getUsernameMetadata(username));

        // EMAIL
        MetadataAttribute emailAttribute = new MetadataAttribute();
        emailAttribute.setUuid("b42ab942-60fd-11ed-9b6a-0242ac120002");
        emailAttribute.setName(META_EMAIL);
        emailAttribute.setDescription("Email");
        emailAttribute.setType(AttributeType.META);
        emailAttribute.setContentType(AttributeContentType.STRING);
        emailAttribute.setContent(List.of(new StringAttributeContent(email)));

        MetadataAttributeProperties emailAttributeProperties = new MetadataAttributeProperties();
        emailAttributeProperties.setVisible(true);
        emailAttributeProperties.setLabel("Email");
        emailAttribute.setProperties(emailAttributeProperties);

        attributes.add(emailAttribute);

        // SAN Attribute
        MetadataAttribute sanAttribute = new MetadataAttribute();
        sanAttribute.setUuid("b42abc58-60fd-11ed-9b6a-0242ac120002");
        sanAttribute.setName(META_SAN);
        sanAttribute.setDescription("SAN");
        sanAttribute.setType(AttributeType.META);
        sanAttribute.setContentType(AttributeContentType.STRING);
        sanAttribute.setContent(List.of(new StringAttributeContent(san)));

        MetadataAttributeProperties sanAttributeProperties = new MetadataAttributeProperties();
        sanAttributeProperties.setVisible(true);
        sanAttributeProperties.setLabel("SAN");
        sanAttribute.setProperties(sanAttributeProperties);

        attributes.add(sanAttribute);

        //Extension
        MetadataAttribute extensionAttribute = new MetadataAttribute();
        extensionAttribute.setUuid("b42abe38-60fd-11ed-9b6a-0242ac120002");
        extensionAttribute.setName(META_EXTENSION);
        extensionAttribute.setDescription("Extension");
        extensionAttribute.setType(AttributeType.META);
        extensionAttribute.setContentType(AttributeContentType.STRING);
        extensionAttribute.setContent(List.of(new StringAttributeContent(extensions)));

        MetadataAttributeProperties extensionAttributeProperties = new MetadataAttributeProperties();
        extensionAttributeProperties.setVisible(true);
        extensionAttributeProperties.setLabel("Extension");
        extensionAttribute.setProperties(extensionAttributeProperties);

        attributes.add(extensionAttribute);

        return attributes;
    }

    private JcaPKCS10CertificationRequest parseCsrToJcaObject(String pkcs10) throws IOException {
        JcaPKCS10CertificationRequest csr;
        try {
            csr = CsrUtil.csrStringToJcaObject(pkcs10);
        } catch (IOException e) {
            logger.debug("Failed to parse CSR, will decode and try again...");
            String decodedPkcs10 = new String(Base64.getDecoder().decode(pkcs10));
            csr = CsrUtil.csrStringToJcaObject(decodedPkcs10);
        }
        return csr;
    }

    private String generateUsername(String usernameGenMethod, String usernamePrefix, String usernamePostfix, JcaPKCS10CertificationRequest csr) throws Exception {
        // the csr comes Base64 encoded
        String username;
        if (usernameGenMethod.equals(UsernameGenMethod.RANDOM.name())) {
            SecureRandom random = new SecureRandom();
            byte[] r = new byte[8];
            random.nextBytes(r);
            username = Base64.getEncoder().encodeToString(r);
        } else if (usernameGenMethod.equals(UsernameGenMethod.CN.name())) {
            if (csr == null) {
                throw new IOException("CSR failed to be decoded!");
            }
            username = getX500Field(COMMON_NAME, csr.getSubject());
        } else {
            String message = "Unsupported username generation method: " + usernameGenMethod;
            logger.debug(message);
            throw new Exception(message);
        }
        if (StringUtils.isNotBlank(username)) {
            if (StringUtils.isNotBlank(usernamePrefix)) {
                username = usernamePrefix + username;
            }
            if (StringUtils.isNotBlank(usernamePostfix)) {
                username = username + usernamePostfix;
            }
        } else {
            String message = "Username is null or empty, username was not properly generated";
            logger.debug(message);
            throw new Exception(message);
        }
        return username;
    }

    @Override
    public void revokeCertificate(String uuid, CertRevocationDto request) throws AccessDeniedException {
        try {
            X509Certificate certificate = CertificateUtil.parseCertificate(request.getCertificate());
            String issuerDn = CertificateUtil.getIssuerDnFromX509Certificate(certificate);
            String serialNumber = CertificateUtil.getSerialNumberFromX509Certificate(certificate);
            int revocationCode = request.getReason().getCode();
            ejbcaService.revokeCertificate(uuid, issuerDn, serialNumber, revocationCode);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String getX500Field(String asn1ObjectIdentifier, X500Name x500Name) {
        RDN[] rdnArray = x500Name.getRDNs(new ASN1ObjectIdentifier(asn1ObjectIdentifier));
        String retVal = null;
        for (RDN item : rdnArray) {
            retVal = item.getFirst().getValue().toString();
        }
        return retVal;
    }
}
