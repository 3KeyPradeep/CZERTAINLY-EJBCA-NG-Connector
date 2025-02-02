package com.czertainly.ca.connector.ejbca.service;

import com.czertainly.api.exception.AlreadyExistException;
import com.czertainly.api.exception.NotFoundException;
import com.czertainly.api.model.client.attribute.RequestAttributeDto;
import com.czertainly.api.model.common.NameAndIdDto;
import com.czertainly.api.model.common.attribute.v2.MetadataAttribute;
import com.czertainly.api.model.connector.v2.CertificateDataResponseDto;
import com.czertainly.ca.connector.ejbca.EjbcaException;
import com.czertainly.ca.connector.ejbca.dto.ejbca.request.SearchCertificatesRestRequestV2;
import com.czertainly.ca.connector.ejbca.dto.ejbca.response.SearchCertificatesRestResponseV2;
import com.czertainly.ca.connector.ejbca.util.EjbcaVersion;
import com.czertainly.ca.connector.ejbca.ws.AuthorizationDeniedException_Exception;
import com.czertainly.ca.connector.ejbca.ws.CADoesntExistsException_Exception;
import com.czertainly.ca.connector.ejbca.ws.CesecoreException_Exception;
import com.czertainly.ca.connector.ejbca.ws.EjbcaException_Exception;
import com.czertainly.ca.connector.ejbca.ws.NotFoundException_Exception;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

public interface EjbcaService {

    void createEndEntity(String authorityUuid, String username, String password, String subjectDn, List<RequestAttributeDto> raProfileAttributes, List<RequestAttributeDto> issueAttributes) throws NotFoundException, AlreadyExistException, EjbcaException;

    void createEndEntityWithMeta(String authorityUuid, String username, String password, String subjectDn, List<RequestAttributeDto> raProfileAttributes, List<MetadataAttribute> metadata) throws NotFoundException, AlreadyExistException;

    void renewEndEntity(String authorityUuid, String username, String password) throws NotFoundException;

    CertificateDataResponseDto issueCertificate(String authorityUuid, String username, String password, String pkcs10) throws NotFoundException, CADoesntExistsException_Exception, EjbcaException_Exception, AuthorizationDeniedException_Exception, NotFoundException_Exception, CesecoreException_Exception;

    void revokeCertificate(String uuid, String issuerDn, String serialNumber, int revocationReason) throws NotFoundException, AccessDeniedException;

    EjbcaVersion getEjbcaVersion(String authorityInstanceUuid) throws NotFoundException;

    SearchCertificatesRestResponseV2 searchCertificates(String authorityInstanceUuid, String restUrl, SearchCertificatesRestRequestV2 request) throws Exception;

    List<NameAndIdDto> getAvailableCas(String authorityInstanceUuid) throws NotFoundException;
}
