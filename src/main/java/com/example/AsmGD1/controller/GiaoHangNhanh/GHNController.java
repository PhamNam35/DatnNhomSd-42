package com.example.AsmGD1.controller.GiaoHangNhanh;

import com.example.AsmGD1.service.GiaoHangNhanh.GHNService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ghn")
public class GHNController {

    @Autowired
    private GHNService ghnService;

    @GetMapping("/calculate-shipping-fee")
    public String calculateShippingFee(
            @RequestParam int fromDistrictId,
            @RequestParam int toDistrictId,
            @RequestParam String toWardCode,
            @RequestParam int weight,
            @RequestParam int length,
            @RequestParam int width,
            @RequestParam int height,
            @RequestParam int serviceId,
            @RequestParam int insuranceValue) {
        return ghnService.calculateShippingFee(fromDistrictId, toDistrictId, toWardCode,
                weight, length, width, height,
                serviceId, insuranceValue);
    }

    @GetMapping("/shipping-fee")
    public APIResponseGHN getShippingFee(
            @RequestParam int fromDistrictId,
            @RequestParam int toDistrictId,
            @RequestParam String toWardCode,
            @RequestParam int weight,
            @RequestParam int length,
            @RequestParam int width,
            @RequestParam int height,
            @RequestParam int serviceId,
            @RequestParam int insuranceValue
    ) {
        try {
            String data = ghnService.calculateShippingFee(fromDistrictId, toDistrictId, toWardCode,
                    weight, length, width, height, serviceId, insuranceValue);
            return new APIResponseGHN(200, "Success", data);
        } catch (Exception e) {
            return new APIResponseGHN(500, e.getMessage(), null);
        }
    }

    @GetMapping("/available-services")
    public APIResponseGHN getAvailableServices(@RequestParam int fromDistrictId, @RequestParam int toDistrictId) {
        try {
            String data = ghnService.getAvailableServices(fromDistrictId, toDistrictId);
            return new APIResponseGHN(200, "Success", data);
        } catch (Exception e) {
            return new APIResponseGHN(500, "GHN Error: " + e.getMessage(), null);
        }
    }



    @GetMapping("/provinces")
    public APIResponseGHN getProvinces() {
        try {
            String data = ghnService.getProvinces();
            return new APIResponseGHN(200, "Success", data);
        } catch (Exception e) {
            return new APIResponseGHN(500, e.getMessage(), null);
        }
    }

    @GetMapping("/districts")
    public APIResponseGHN getDistricts(@RequestParam int provinceId) {
        try {
            String data = ghnService.getDistricts(provinceId);
            return new APIResponseGHN(200, "Success", data);
        } catch (Exception e) {
            return new APIResponseGHN(500, e.getMessage(), null);
        }
    }

    @GetMapping("/wards")
    public APIResponseGHN getWards(@RequestParam int districtId) {
        try {
            String data = ghnService.getWards(districtId);
            return new APIResponseGHN(200, "Success", data);
        } catch (Exception e) {
            return new APIResponseGHN(500, e.getMessage(), null);
        }
    }

}