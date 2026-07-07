package com.example.upagain.api


object Endpoints {
    // Img Endpoints
    const val IMAGES = "mobile/images"

    // Auth Endpoints
    const val LOGIN = "login"
    const val REFRESH = "refresh"

    // Account Endpoints
    const val ACCOUNT_DETAILS = "accounts/{id}"
    const val ACCOUNT_UPDATE = "accounts/{id}/update"
    const val PASSWORD_UPDATE = "accounts/{id}/password"
    const val AVATAR_UPDATE = "accounts/{id}/avatar"
    const val PRO_ANALYTICS = "accounts/{id}/pro-analytics"

    // Container Endpoints
    const val CONTAINER_OPEN = "containers/{id}/open"

    // Post Endpoints
    const val POST_ALL = "posts"
    const val POST_CREATE = "posts"
    const val POST_ME = "posts/me"
    const val POST_SAVED = "posts/saved"
    const val POST_DETAILS = "posts/{id}"
    const val POST_UPDATE = "posts/{id}"
    const val POST_DELETE = "posts/{id}"
    const val POST_SAVE = "posts/{id}/save"
    const val POST_VIEW = "posts/{id}/view"
    const val POST_LIKE = "posts/{id}/like"
    const val POST_STEP = "posts/{id}/steps"
    const val POST_STEP_UPDATE = "posts/steps/{id}"
    const val POST_STEP_DELETE = "posts/steps/{id}"
    const val POST_STEP_REORDER = "/posts/steps/{id}/reorder"

    // Comment Endpoints
    const val COMMENTS_ALL = "posts/{id}/comments"
    const val COMMENTS_NEW = "posts/{id}/comments"
    const val COMMENTS_LIKE = "comments/{id}/like"
    const val COMMENTS_DELETE = "comments/{id}"

    // Step Endpoints
    const val STEPS_GET = "posts/{id}/steps"
    const val STEPS_CREATE = "posts/{id}/steps"
    const val STEPS_EDIT = "posts/steps/{id}"
    const val STEPS_REORDER = "posts/steps/{id}/reorder"
    const val STEPS_DELETE = "posts/steps/{id}"

    // Ad Endpoints
    const val ADS_CREATE = "ads"
    const val ADS_UPDATE = "ads/{id}"
    const val ADS_DELETE = "ads/{id}"

    // Shop Endpoints
    const val SHOP_ITEM_ALL = "items"
    const val SHOP_ITEM_ME = "items/me"
    const val SHOP_ITEM_DETAILS = "items/{id}"
    const val SHOP_ITEM_DELETE = "items/{id}"
    const val SHOP_ITEM_TRANSACTIONS = "items/{id}/transactions"
    const val SHOP_ITEM_LATEST_TRANSACTION = "items/{id}/transactions/latest"
    const val SHOP_ITEM_RESERVE = "items/{id}/reserve"
    const val SHOP_ITEM_PURCHASE = "items/{id}/purchase"
    const val SHOP_ITEM_CANCEL_RESERVE = "items/{id}/cancel"
    const val SHOP_LISTING_DETAILS = "listings/{id}"
    const val SHOP_DEPOSIT_DETAILS = "deposits/{id}"
    const val SHOP_DEPOSIT_CODES = "codes/{id}"

    // Stripe Endpoints
    const val STRIPE_VERIFY = "payments/verify"

    // Finance
    const val FINANCE_SETTING = "finance/settings/{key}"
}