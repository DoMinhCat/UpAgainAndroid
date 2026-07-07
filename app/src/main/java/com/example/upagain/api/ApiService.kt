package com.example.upagain.api

import com.example.upagain.model.LoginRequest
import com.example.upagain.model.TokenResponse
import com.example.upagain.model.account.AccountDetailsResponse
import com.example.upagain.model.account.AccountUpdateRequest
import com.example.upagain.model.account.PasswordUpdateRequest
import com.example.upagain.model.ads.CreateAdsRequest
import com.example.upagain.model.ads.CreateAdsResponse
import com.example.upagain.model.comment.CommentDetailsResponse
import com.example.upagain.model.comment.CommentPaginationResponse
import com.example.upagain.model.comment.CreateCommentRequest
import com.example.upagain.model.comment.LikeCommentResponse
import com.example.upagain.model.dashboard.ProAnalyticsResponse
import com.example.upagain.model.finance.FinanceKeyEnum
import com.example.upagain.model.item.ItemStatus
import com.example.upagain.model.item.MyItemsResponse
import com.example.upagain.model.post.LikePostResponse
import com.example.upagain.model.post.PostDetailsResponse
import com.example.upagain.model.post.PostPaginationResponse
import com.example.upagain.model.post.ProjectStepResponse
import com.example.upagain.model.post.SavePostResponse
import com.example.upagain.model.post.ViewPostResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface ApiService {
    // AUTH
    @POST(Endpoints.LOGIN)
    fun login(@Body request: LoginRequest): Call<TokenResponse>

    @POST(Endpoints.REFRESH)
    fun refresh(): Call<TokenResponse>

    // ACCOUNT
    @GET(Endpoints.ACCOUNT_DETAILS)
    fun getAccountDetails(@Path("id") id: Int): Call<AccountDetailsResponse>

    @Multipart
    @POST(Endpoints.AVATAR_UPDATE)
    fun uploadAvatar(
        @Path("id") idAccount: Int, @Part avatar: MultipartBody.Part
    ): Call<Unit>

    @PATCH(Endpoints.ACCOUNT_UPDATE)
    fun updateAccount(@Path("id") id: Int, @Body request: AccountUpdateRequest): Call<Unit>

    @PATCH(Endpoints.PASSWORD_UPDATE)
    fun updatePassword(@Path("id") id: Int, @Body request: PasswordUpdateRequest): Call<Unit>

    @DELETE(Endpoints.ACCOUNT_DETAILS)
    fun deleteAccount(@Path("id") id: Int): Call<Unit>

    // POST aka COMMUNITY
    @Multipart
    @POST(Endpoints.POST_CREATE)
    fun createPost(
        @Part("title") title: RequestBody,
        @Part("content") content: RequestBody,
        @Part("category") category: RequestBody,
        @Part images: List<MultipartBody.Part>
    ): Call<Unit>

    @Multipart
    @PUT(Endpoints.POST_UPDATE)
    fun updatePost(
        @Path("id") id: Int,
        @Part("title") title: RequestBody,
        @Part("content") content: RequestBody,
        @Part("category") category: RequestBody,
        @Part("end_date") endDate: RequestBody?,
        @Part newImages: List<MultipartBody.Part>,
        @Part existingImages: List<MultipartBody.Part>
    ): Call<Unit>

    @POST(Endpoints.POST_VIEW)
    fun viewPost(@Path("id") id: Int): Call<ViewPostResponse>

    @POST(Endpoints.POST_LIKE)
    fun likePost(@Path("id") id: Int): Call<LikePostResponse>

    @POST(Endpoints.POST_SAVE)
    fun savePost(@Path("id") id: Int): Call<SavePostResponse>

    @GET(Endpoints.POST_ALL)
    fun getAllPosts(@QueryMap options: Map<String, String>): Call<PostPaginationResponse>

    @GET(Endpoints.POST_ME)
    fun getMyPosts(@QueryMap options: Map<String, String>): Call<PostPaginationResponse>

    @GET(Endpoints.POST_SAVED)
    fun getSavedPosts(@QueryMap options: Map<String, String>): Call<PostPaginationResponse>

    @GET(Endpoints.POST_DETAILS)
    fun getPostDetails(@Path("id") id: Int): Call<PostDetailsResponse>

    @GET(Endpoints.STEPS_GET)
    fun getProjectSteps(@Path("id") id: Int): Call<List<ProjectStepResponse>>

    @Multipart
    @POST(Endpoints.STEPS_CREATE)
    fun createProjectStep(
        @Path("id") idPost: Int,
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part itemIds: List<MultipartBody.Part>,
        @Part images: List<MultipartBody.Part>
    ): Call<Unit>

    @Multipart
    @PUT(Endpoints.STEPS_EDIT)
    fun updateProjectStep(
        @Path("id") idStep: Int,
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part existingImages: List<MultipartBody.Part>,
        @Part newImages: List<MultipartBody.Part>,
        @Part itemIds: List<MultipartBody.Part>
    ): Call<Unit>

    @DELETE(Endpoints.POST_DELETE)
    fun deletePost(@Path("id") id: Int): Call<Unit>

    // STEP
    @DELETE(Endpoints.STEPS_DELETE)
    fun deleteProjectStep(@Path("id") id: Int): Call<Unit>

    // COMMENTS
    @POST(Endpoints.COMMENTS_NEW)
    fun createComment(
        @Path("id") id: Int, @Body request: CreateCommentRequest
    ): Call<CommentDetailsResponse>

    @POST(Endpoints.COMMENTS_LIKE)
    fun likeComment(@Path("id") id: Int): Call<LikeCommentResponse>

    @GET(Endpoints.COMMENTS_ALL)
    fun getPostComments(
        @Path("id") id: Int, @QueryMap options: Map<String, String>
    ): Call<CommentPaginationResponse>

    @DELETE(Endpoints.COMMENTS_DELETE)
    fun deleteComment(@Path("id") id: Int): Call<Unit>

    // ADS
    @POST(Endpoints.ADS_CREATE)
    fun createAds(
        @Body request: CreateAdsRequest
    ): Call<CreateAdsResponse>

    @DELETE(Endpoints.ADS_DELETE)
    fun deleteAds(@Path("id") id: Int): Call<Unit>

    // CONTAINER
    @Multipart
    @POST(Endpoints.CONTAINER_OPEN)
    fun openContainer(
        @Path("id") id: Int,
        @Part barcode: MultipartBody.Part?,
        @Part("code6digit") digitCode: RequestBody?
    ): Call<Unit>

    // FINANCIAL SETTINGS
    @GET(Endpoints.FINANCE_SETTING)
    fun getFinanceSetting(@Path("key") key: FinanceKeyEnum): Call<ResponseBody>


    // SHOP
    @GET(Endpoints.SHOP_ITEM_ME)
    fun getMyItems(
        @Query("page") page: Int? = 1,
        @Query("limit") limit: Int? = 10,
        @Query("status") status: String? = ItemStatus.APPROVED.value
    ): Call<MyItemsResponse>

    @GET(Endpoints.SHOP_ITEM_ME)
    fun getMyItemsPaginated(
        @QueryMap options: Map<String, String>
    ): Call<MyItemsResponse>

    @GET(Endpoints.SHOP_ITEM_ALL)
    fun getAllItems(
        @QueryMap options: Map<String, String>
    ): Call<MyItemsResponse>

    @GET(Endpoints.SHOP_ITEM_DETAILS)
    fun getItemDetails(@Path("id") id: Int): Call<com.example.upagain.model.item.ItemDetailResponse>

    @GET(Endpoints.SHOP_LISTING_DETAILS)
    fun getListingDetails(@Path("id") id: Int): Call<com.example.upagain.model.item.ListingDetailResponse>

    @GET(Endpoints.SHOP_DEPOSIT_DETAILS)
    fun getDepositDetails(@Path("id") id: Int): Call<com.example.upagain.model.item.DepositDetailResponse>

    @GET(Endpoints.SHOP_DEPOSIT_CODES)
    fun getDepositCodes(@Path("id") id: Int): Call<List<com.example.upagain.model.transaction.BarcodeResponse>>

    @DELETE(Endpoints.SHOP_ITEM_DELETE)
    fun deleteItem(@Path("id") id: Int): Call<ResponseBody>

    // TRANSACTIONS
    @GET(Endpoints.SHOP_ITEM_TRANSACTIONS)
    fun getItemTransactions(
        @Path("id") id: Int,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): Call<com.example.upagain.model.transaction.TransactionsPaginationResponse>

    @GET(Endpoints.SHOP_ITEM_LATEST_TRANSACTION)
    fun getLatestTransactionOfPro(@Path("id") id: Int): Call<com.example.upagain.model.transaction.TransactionResponse>

    @POST(Endpoints.SHOP_ITEM_RESERVE)
    fun reserveItem(@Path("id") id: Int): Call<ResponseBody>

    @POST(Endpoints.SHOP_ITEM_CANCEL_RESERVE)
    fun cancelItemReservation(@Path("id") id: Int): Call<ResponseBody>

    @POST(Endpoints.SHOP_ITEM_PURCHASE)
    fun purchaseItem(
        @Path("id") id: Int,
        @Body payload: com.example.upagain.model.transaction.ItemPurchaseRequest
    ): Call<ResponseBody>

    @GET(Endpoints.PRO_ANALYTICS)
    fun getProAnalytics(
        @Path("id") id: Int,
        @Query("timeframe") timeframe: String?
    ): Call<ProAnalyticsResponse>
}
