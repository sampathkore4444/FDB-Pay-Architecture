package com.fdbpay.merchant.service.service;

import com.fdbpay.merchant.service.dto.request.ProductRequest;
import com.fdbpay.merchant.service.dto.response.ProductResponse;
import com.fdbpay.merchant.service.model.Product;
import com.fdbpay.merchant.service.model.enums.ActiveStatus;
import com.fdbpay.merchant.service.repository.MerchantRepository;
import com.fdbpay.merchant.service.repository.ProductRepository;
import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogService {

    private final ProductRepository productRepository;
    private final MerchantRepository merchantRepository;
    private final AuditService auditService;

    public List<ProductResponse> listProducts(UUID merchantId) {
        requireMerchant(merchantId);
        return productRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)
                .stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public ProductResponse createProduct(UUID merchantId, ProductRequest request) {
        requireMerchant(merchantId);
        Product product = Product.builder()
                .merchantId(merchantId)
                .name(request.getName())
                .price(request.getPrice())
                .description(request.getDescription())
                .category(request.getCategory())
                .imageUrl(request.getImageUrl())
                .quantity(request.getQuantity() == null ? 0L : request.getQuantity())
                .lowStockThreshold(request.getLowStockThreshold() == null ? 0L : request.getLowStockThreshold())
                .status(ActiveStatus.ACTIVE)
                .build();
        product = productRepository.save(product);
        auditService.log(merchantId, "OWNER", null, null, "CREATE", "PRODUCT", product.getId().toString(),
                "Added product '" + product.getName() + "' priced " + product.getPrice() + " MMK");
        return mapToResponse(product);
    }

    @Transactional
    public ProductResponse updateProduct(UUID merchantId, UUID productId, ProductRequest request) {
        Product product = getOwned(merchantId, productId);
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        product.setImageUrl(request.getImageUrl());
        product.setQuantity(request.getQuantity() == null ? product.getQuantity() : request.getQuantity());
        product.setLowStockThreshold(request.getLowStockThreshold() == null ? product.getLowStockThreshold() : request.getLowStockThreshold());
        product = productRepository.save(product);
        auditService.log(merchantId, "OWNER", null, null, "UPDATE", "PRODUCT", productId.toString(), "Updated product '" + product.getName() + "'");
        return mapToResponse(product);
    }

    public List<ProductResponse> lowStock(UUID merchantId) {
        requireMerchant(merchantId);
        return productRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId).stream()
                .filter(p -> p.getStatus() == ActiveStatus.ACTIVE
                        && p.getLowStockThreshold() != null && p.getLowStockThreshold() > 0
                        && p.getQuantity() != null && p.getQuantity() <= p.getLowStockThreshold())
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public void deleteProduct(UUID merchantId, UUID productId) {
        Product product = getOwned(merchantId, productId);
        productRepository.delete(product);
        auditService.log(merchantId, "OWNER", null, null, "DELETE", "PRODUCT", productId.toString(), "Deleted product '" + product.getName() + "'");
    }

    private Product getOwned(UUID merchantId, UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId.toString()));
        if (!product.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Product does not belong to this merchant");
        }
        return product;
    }

    private void requireMerchant(UUID merchantId) {
        merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .merchantId(product.getMerchantId())
                .name(product.getName())
                .price(product.getPrice())
                .description(product.getDescription())
                .category(product.getCategory())
                .imageUrl(product.getImageUrl())
                .quantity(product.getQuantity())
                .lowStockThreshold(product.getLowStockThreshold())
                .status(product.getStatus())
                .createdAt(product.getCreatedAt())
                .build();
    }
}
