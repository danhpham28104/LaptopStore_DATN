package com.laptopstore.laptopstore.enums;

/**
 * Các loại sự kiện được theo dõi bởi hệ thống Analytics.
 *
 * <p>Phân nhóm sự kiện:
 * <ul>
 *   <li>Product: PRODUCT_VIEW, PRODUCT_CLICK</li>
 *   <li>Search: SEARCH, FILTER_APPLIED</li>
 *   <li>Wishlist: ADD_TO_WISHLIST, REMOVE_FROM_WISHLIST</li>
 *   <li>Cart: ADD_TO_CART, REMOVE_FROM_CART, UPDATE_CART</li>
 *   <li>Checkout: BEGIN_CHECKOUT, APPLY_VOUCHER</li>
 *   <li>Order: ORDER_CREATED, ORDER_CANCELLED</li>
 *   <li>Payment: PAYMENT_SUCCESS, PAYMENT_FAILED</li>
 *   <li>AI: AI_CHAT, AI_PRODUCT_RECOMMENDED, AI_PRODUCT_CLICK</li>
 *   <li>Review: REVIEW_CREATED</li>
 * </ul>
 */
public enum EventType {

    // ── Product ─────────────────────────────────────────────────────────────
    PRODUCT_VIEW,
    PRODUCT_CLICK,

    // ── Search ──────────────────────────────────────────────────────────────
    /** metadata: { keyword, resultCount } */
    SEARCH,
    FILTER_APPLIED,

    // ── Wishlist ─────────────────────────────────────────────────────────────
    ADD_TO_WISHLIST,
    REMOVE_FROM_WISHLIST,

    // ── Cart ─────────────────────────────────────────────────────────────────
    ADD_TO_CART,
    REMOVE_FROM_CART,
    UPDATE_CART,

    // ── Checkout ─────────────────────────────────────────────────────────────
    BEGIN_CHECKOUT,
    /** metadata: { voucherCode, success, discountAmount } */
    APPLY_VOUCHER,

    // ── Order ─────────────────────────────────────────────────────────────────
    ORDER_CREATED,
    ORDER_CANCELLED,

    // ── Payment ───────────────────────────────────────────────────────────────
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,

    // ── AI ───────────────────────────────────────────────────────────────────
    AI_CHAT,
    /** metadata: { recommendedProductIds, confidenceScore } */
    AI_PRODUCT_RECOMMENDED,
    AI_PRODUCT_CLICK,

    // ── Review ───────────────────────────────────────────────────────────────
    REVIEW_CREATED
}
