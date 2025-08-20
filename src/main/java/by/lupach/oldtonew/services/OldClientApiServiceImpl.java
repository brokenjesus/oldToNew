package by.lupach.oldtonew.services;

import by.lupach.oldtonew.dtos.OldClientDto;
import by.lupach.oldtonew.dtos.OldNoteDto;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;

@Service
public class OldClientApiServiceImpl implements OldClientApi {
    private final RestTemplate oldSystemRestTemplate;

    public OldClientApiServiceImpl(RestTemplate oldSystemRestTemplate) {
        this.oldSystemRestTemplate = oldSystemRestTemplate;
    }

    @Override
    public List<OldClientDto> getAllClients() {
        try {
            OldClientDto[] arr = oldSystemRestTemplate.postForObject("/clients", null, OldClientDto[].class);
            return arr == null ? List.of() : Arrays.asList(arr);
        } catch (RestClientException e) {
            throw new RuntimeException("Failed to fetch clients from old system", e);
        }
    }

    @Override
    public List<OldNoteDto> getClientNotes(String agency, UUID clientGuid, LocalDate from, LocalDate to) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("agency", agency);
        payload.put("dateFrom", from.toString());
        payload.put("dateTo", to.toString());
        payload.put("clientGuid", clientGuid);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            OldNoteDto[] arr = oldSystemRestTemplate.postForObject("/notes", new HttpEntity<>(payload, headers), OldNoteDto[].class);
            return arr == null ? List.of() : Arrays.asList(arr);
        } catch (RestClientException e) {
            throw new RuntimeException("Failed to fetch notes for clientGuid=" + clientGuid + ", agency=" + agency, e);
        }
    }
}