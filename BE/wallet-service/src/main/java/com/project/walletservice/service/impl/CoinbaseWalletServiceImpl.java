package com.project.walletservice.service.impl;

import com.project.walletservice.common.BaseResponse;
import com.project.walletservice.common.Const;
import com.project.walletservice.model.Wallet;
import com.project.walletservice.payload.request.client.CreateWalletRequest;
import com.project.walletservice.payload.request.coinbase.CoinbaseApiCreateWalletRequest;
import com.project.walletservice.payload.response.coinbase.CoinbaseApiCreateWalletResponse;
import com.project.walletservice.repository.WalletRepository;
import com.project.walletservice.service.CoinbaseWalletService;
import com.project.walletservice.service.OAuthService;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CoinbaseWalletServiceImpl implements CoinbaseWalletService {
    Dotenv dotenv = Dotenv.configure()
            .directory(System.getProperty("user.dir"))  // Ensure it loads from the root of wallet-service
            .filename(".env")  // Ensure correct filename
            .ignoreIfMissing()  // Prevents crashing if .env is missing
            .load();

    private final RestTemplate restTemplate;
    private final OAuthService oAuthService;
    private final WalletRepository walletRepository;
    private final String oAuthUrl =
            "https://login.coinbase.com/oauth2/auth?" +
            "response_type=" + "code" +
            "&client_id=" + dotenv.get("COINBASE_OAUTH_CLIENT_ID") +
            "&redirect_uri=" +  dotenv.get("COINBASE_OAUTH_REDIRECT_URI") +
            "&state=" + dotenv.get("COINBASE_OAUTH_STATE") +
            "&scope=" + dotenv.get("COINBASE_ALL_SCOPES");

    @Override
    public BaseResponse<?> createWallet(String userId, CreateWalletRequest request) {

        // 1) Get a valid Coinbase access token for this user
        String accessToken = oAuthService.getValidAccessToken(userId);
        if (accessToken == null) {
            // user not linked or token can't be refreshed
            return new BaseResponse<>(
                Const.STATUS_RESPONSE.ERROR,
                "User not linked to Coinbase or token invalid",
                oAuthUrl
            );
        }

        // 2) Prepare the request body for Coinbase
        CoinbaseApiCreateWalletRequest requestBody = new CoinbaseApiCreateWalletRequest(
            request.getNetworkId(),
            request.isUseServerSigner()
        );

        // 3) Build headers
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CoinbaseApiCreateWalletRequest> entity = new HttpEntity<>(requestBody, headers);

        // 4) Call the Coinbase "create wallet" endpoint
        String coinbaseUrl = "https://api.cdp.coinbase.com/platform/v1/wallets";
        // Adjust if your endpoint differs per actual Coinbase docs
        ResponseEntity<CoinbaseApiCreateWalletResponse> response;

        try {
            response = restTemplate.postForEntity(
                    coinbaseUrl,
                    entity,
                    CoinbaseApiCreateWalletResponse.class
            );
        } catch (RestClientException ex) {
            // handle error
            return new BaseResponse<>(
                Const.STATUS_RESPONSE.ERROR,
                "Coinbase API request failed:"  + ex.getMessage(),
                ""
            );
        }

        // 5) Interpret the response
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            CoinbaseApiCreateWalletResponse responseBody = response.getBody();
            Wallet newWallet = new Wallet(
                responseBody.getId(),
                request.getName(),
                userId,
                responseBody.getNetwork_id(),
                responseBody.getDefault_address(),
                responseBody.getFeature_set(),
                responseBody.getServer_signer_status(),
                Instant.now(),
                Instant.now()
            );

            walletRepository.save(newWallet);           // Save new wallet

            return new BaseResponse<>(
                Const.STATUS_RESPONSE.SUCCESS,
                "Wallet created successfully!",
                newWallet
            );
        } else {
            return new BaseResponse<>(
                Const.STATUS_RESPONSE.ERROR,
                "Failed to create wallet. Status: "  + response.getStatusCode(),
                ""
            );
        }
    }
}
