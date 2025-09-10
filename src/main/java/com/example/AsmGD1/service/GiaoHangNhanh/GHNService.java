package com.example.AsmGD1.service.GiaoHangNhanh;

import com.example.AsmGD1.config.GHNConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class GHNService {

    @Autowired
    private GHNConfig ghnConfig;

    @Autowired
    private RestTemplate restTemplate;

    public String calculateShippingFee(int fromDistrictId, int toDistrictId, String toWardCode,
                                       int weight, int length, int width, int height,
                                       int serviceId, int insuranceValue) {
        String url = ghnConfig.getUrl() + "/v2/shipping-order/fee";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", ghnConfig.getToken());
        headers.set("ShopId", ghnConfig.getShopId());
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = String.format("""
        {
            "from_district_id": %d,
            "to_district_id": %d,
            "to_ward_code": "%s",
            "service_id": %d,
            "insurance_value": %d,
            "weight": %d,
            "length": %d,
            "width": %d,
            "height": %d
        }
        """, fromDistrictId, toDistrictId, toWardCode, serviceId, insuranceValue, weight, length, width, height);

        HttpEntity<String> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            throw new RuntimeException("GHN Fee API error: " + response.getStatusCode());
        }
    }


    public String getProvinces() {
        String url = ghnConfig.getUrl() + "/master-data/province";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", ghnConfig.getToken());
        headers.set("ShopId", ghnConfig.getShopId()); // Thêm ShopId nếu yêu cầu
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            throw new RuntimeException("Failed to fetch provinces: " + response.getStatusCode() + " - " + response.getBody());
        }
    }

    public String getDistricts(int provinceId) {
        String url = ghnConfig.getUrl() + "/master-data/district?province_id=" + provinceId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", ghnConfig.getToken());
        headers.set("ShopId", ghnConfig.getShopId()); // Thêm ShopId nếu yêu cầu
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            throw new RuntimeException("Failed to fetch districts: " + response.getStatusCode() + " - " + response.getBody());
        }
    }

    public String getWards(int districtId) {
        String url = ghnConfig.getUrl() + "/master-data/ward?district_id=" + districtId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", ghnConfig.getToken());
        headers.set("ShopId", ghnConfig.getShopId()); // ShopId vẫn nên có nếu GHN yêu cầu
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            throw new RuntimeException("Failed to fetch wards: " + response.getStatusCode() + " - " + response.getBody());
        }
    }

    public String getAvailableServices(int fromDistrictId, int toDistrictId) {
        String url = "https://online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/available-services";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", ghnConfig.getToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("shop_id", Integer.parseInt(ghnConfig.getShopId()));
        body.put("from_district", fromDistrictId);
        body.put("to_district", toDistrictId);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            throw new RuntimeException("Failed to fetch available services: " + response.getStatusCode());
        }
    }

}