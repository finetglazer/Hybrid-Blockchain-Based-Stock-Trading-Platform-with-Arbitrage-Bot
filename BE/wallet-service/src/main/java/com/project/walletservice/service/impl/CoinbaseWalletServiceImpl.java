package com.project.walletservice.service.impl;

import com.project.walletservice.payload.request.CreateWalletRequest;
import com.project.walletservice.payload.response.CreateWalletResponse;
import com.project.walletservice.service.CoinbaseWalletService;
import com.project.walletservice.service.OAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class CoinbaseWalletServiceImpl implements CoinbaseWalletService {

    private final RestTemplate restTemplate;
    private final OAuthService oAuthService;

    @Override
    public CreateWalletResponse createWallet(String userId, CreateWalletRequest request) {

        // 1) Get a valid Coinbase access token for this user
        String accessToken = oAuthService.getValidAccessToken(userId);
        if (accessToken == null) {
            // user not linked or token can't be refreshed
            CreateWalletResponse failResponse = new CreateWalletResponse();
            failResponse.setMessage("User not linked to Coinbase or token invalid.");
            return failResponse;
        }

        // 2) Prepare the request body for Coinbase
        // e.g. fields: name, network, signer
        // Let's assume we have a class CreateWalletApiBody that matches Coinbase's expected JSON
        CreateWalletApiBody requestBody = new CreateWalletApiBody(
                request.getWalletName(),
                request.getNetwork(),
                request.getSigner()
        );

        // 3) Build headers
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateWalletApiBody> entity = new HttpEntity<>(requestBody, headers);

        // 4) Call the Coinbase "create wallet" endpoint
        String coinbaseUrl = "https://api.coinbase.com/v2/wallets";
        // Adjust if your endpoint differs per actual Coinbase docs
        ResponseEntity<CreateWalletApiResponse> response;

        try {
            response = restTemplate.postForEntity(
                    coinbaseUrl,
                    entity,
                    CreateWalletApiResponse.class
            );
        } catch (RestClientException ex) {
            // handle error
            CreateWalletResponse failResponse = new CreateWalletResponse();
            failResponse.setMessage("Coinbase API request failed: " + ex.getMessage());
            return failResponse;
        }

        // 5) Interpret the response
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            String newWalletId = response.getBody().getWalletId(); // assume getWalletId() is defined

            CreateWalletResponse successResponse = new CreateWalletResponse();
            successResponse.setCoinbaseWalletId(newWalletId);
            successResponse.setMessage("Wallet created successfully!");
            return successResponse;
        } else {
            CreateWalletResponse failResponse = new CreateWalletResponse();
            failResponse.setMessage("Failed to create wallet. Status: " + response.getStatusCode());
            return failResponse;
        }
    }

    // Example "inner" request/response classes for Coinbase API
    // In real code, they'd likely be separate files or you might map them dynamically
    private static class CreateWalletApiBody {
        private String name;
        private String network;
        private String signer;

        public CreateWalletApiBody(String name, String network, String signer) {
            this.name = name;
            this.network = network;
            this.signer = signer;
        }
        // getters/setters if needed
    }

    private static class CreateWalletApiResponse {
        private String walletId;  // name this to match the actual Coinbase JSON field

        public String getWalletId() {
            return walletId;
        }
        public void setWalletId(String walletId) {
            this.walletId = walletId;
        }
    }
}
