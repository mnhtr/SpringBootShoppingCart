package com.example.demo.model;

import java.util.ArrayList;
import java.util.List;

public class CartInfo {

    private int orderNum;
    private CustomerInfo customerInfo;
    private final List<CartLineInfo> cartLines = new ArrayList<>();

    public int getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(int orderNum) {
        this.orderNum = orderNum;
    }

    public CustomerInfo getCustomerInfo() {
        return customerInfo;
    }

    public void setCustomerInfo(CustomerInfo customerInfo) {
        this.customerInfo = customerInfo;
    }

    public List<CartLineInfo> getCartLines() {
        return cartLines;
    }

    public void addProduct(ProductInfo productInfo, int quantity) {
        CartLineInfo line = findLineByCode(productInfo.getCode());

        if (line == null) {
            line = new CartLineInfo();
            line.setProductInfo(productInfo);
            this.cartLines.add(line);
        }

        int newQuantity = line.getQuantity() + quantity;
        if (newQuantity > 0) {
            line.setQuantity(newQuantity);
        } else {
            this.cartLines.remove(line);
        }
    }

    public void updateProduct(String code, int quantity) {
        CartLineInfo line = findLineByCode(code);

        if (line != null) {
            if (quantity > 0) {
                line.setQuantity(quantity);
            } else {
                this.cartLines.remove(line);
            }
        }
    }

    public void removeProduct(ProductInfo productInfo) {
        CartLineInfo line = findLineByCode(productInfo.getCode());
        if (line != null) {
            this.cartLines.remove(line);
        }
    }

    public boolean isEmpty() {
        return this.cartLines.isEmpty();
    }

    public boolean isValidCustomer() {
        return this.customerInfo != null && this.customerInfo.isValid();
    }

    public int getQuantityTotal() {
        return this.cartLines.stream()
                .mapToInt(CartLineInfo::getQuantity)
                .sum();
    }

    public double getAmountTotal() {
        return this.cartLines.stream()
                .mapToDouble(CartLineInfo::getAmount)
                .sum();
    }

    public void updateQuantity(CartInfo cartForm) {
        if (cartForm != null) {
            for (CartLineInfo line : cartForm.getCartLines()) {
                updateProduct(line.getProductInfo().getCode(), line.getQuantity());
            }
        }
    }

    private CartLineInfo findLineByCode(String code) {
        return this.cartLines.stream()
                .filter(line -> line.getProductInfo().getCode().equals(code))
                .findFirst()
                .orElse(null);
    }
}
