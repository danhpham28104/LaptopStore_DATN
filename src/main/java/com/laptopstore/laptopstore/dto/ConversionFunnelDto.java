package com.laptopstore.laptopstore.dto;

public class ConversionFunnelDto {

    private Long productViews;
    private Long addToCart;
    private Long beginCheckout;
    private Long orderCreated;
    private Long paymentSuccess;

    private Double viewToCartRate;
    private Double cartToCheckoutRate;
    private Double checkoutToOrderRate;
    private Double orderToPaymentRate;
    private Double overallConversionRate;

    public ConversionFunnelDto() {}

    public ConversionFunnelDto(Long productViews, Long addToCart, Long beginCheckout, Long orderCreated, Long paymentSuccess) {
        this.productViews = productViews != null ? productViews : 0L;
        this.addToCart = addToCart != null ? addToCart : 0L;
        this.beginCheckout = beginCheckout != null ? beginCheckout : 0L;
        this.orderCreated = orderCreated != null ? orderCreated : 0L;
        this.paymentSuccess = paymentSuccess != null ? paymentSuccess : 0L;

        this.viewToCartRate = this.productViews > 0 ? (double) this.addToCart / this.productViews * 100 : 0.0;
        this.cartToCheckoutRate = this.addToCart > 0 ? (double) this.beginCheckout / this.addToCart * 100 : 0.0;
        this.checkoutToOrderRate = this.beginCheckout > 0 ? (double) this.orderCreated / this.beginCheckout * 100 : 0.0;
        this.orderToPaymentRate = this.orderCreated > 0 ? (double) this.paymentSuccess / this.orderCreated * 100 : 0.0;
        this.overallConversionRate = this.productViews > 0 ? (double) this.paymentSuccess / this.productViews * 100 : 0.0;
    }

    public Long getProductViews() { return productViews; }
    public void setProductViews(Long productViews) { this.productViews = productViews; }

    public Long getAddToCart() { return addToCart; }
    public void setAddToCart(Long addToCart) { this.addToCart = addToCart; }

    public Long getBeginCheckout() { return beginCheckout; }
    public void setBeginCheckout(Long beginCheckout) { this.beginCheckout = beginCheckout; }

    public Long getOrderCreated() { return orderCreated; }
    public void setOrderCreated(Long orderCreated) { this.orderCreated = orderCreated; }

    public Long getPaymentSuccess() { return paymentSuccess; }
    public void setPaymentSuccess(Long paymentSuccess) { this.paymentSuccess = paymentSuccess; }

    public Double getViewToCartRate() { return viewToCartRate; }
    public Double getCartToCheckoutRate() { return cartToCheckoutRate; }
    public Double getCheckoutToOrderRate() { return checkoutToOrderRate; }
    public Double getOrderToPaymentRate() { return orderToPaymentRate; }
    public Double getOverallConversionRate() { return overallConversionRate; }
}
