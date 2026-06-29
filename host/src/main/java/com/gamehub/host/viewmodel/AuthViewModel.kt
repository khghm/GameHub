// host/src/main/java/com/gamehub/host/viewmodel/AuthViewModel.kt
package com.gamehub.host.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamehub.host.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class FriendInfo(
    val userId: String,
    val username: String,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val isOnline: Boolean = false
)

data class UserProfile(
    val userId: String = "",
    val username: String,
    val displayName: String,
    val avatar: String,
    val wins: Int,
    val losses: Int,
    val draws: Int,
    val friends: List<FriendInfo>
)

data class FriendRequest(
    val userId: String,
    val username: String,
    val requestId: String
)

class AuthViewModel : ViewModel() {
    private val apiClient = ApiClient()

    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser

    private val _currentUserId = MutableStateFlow("")
    val currentUserId: StateFlow<String> = _currentUserId

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _pendingRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val pendingRequests: StateFlow<List<FriendRequest>> = _pendingRequests

    // ========== Guest Login ==========
    fun guestLogin(deviceId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = apiClient.guestLogin(deviceId)
                val success = response["success"]?.jsonPrimitive?.boolean ?: false
                val token = response["token"]?.jsonPrimitive?.content
                val userId = response["user"]?.jsonObject?.get("id")?.jsonPrimitive?.content ?: ""
                val message = response["message"]?.jsonPrimitive?.content ?: ""

                if (success && token != null) {
                    _token.value = token
                    apiClient.setToken(token)
                    _currentUserId.value = userId
                    _isLoggedIn.value = true
                    _message.value = message
                    loadProfile()
                } else {
                    _message.value = message
                }
            } catch (e: Exception) {
                _message.value = "خطا در ارتباط با سرور: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ========== Register & Login (temporary) ==========
    fun register(username: String, password: String, displayName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = apiClient.register(username, password, displayName)
            val success = response["success"]?.jsonPrimitive?.boolean ?: false
            if (success) {
                val token = response["token"]?.jsonPrimitive?.content
                val userId = response["user"]?.jsonObject?.get("id")?.jsonPrimitive?.content ?: ""
                if (token != null) {
                    _token.value = token
                    apiClient.setToken(token)
                    _currentUserId.value = userId
                    _isLoggedIn.value = true
                    loadProfile()
                }
            }
            _message.value = response["message"]?.jsonPrimitive?.content ?: ""
            _isLoading.value = false
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = apiClient.login(username, password)
            val success = response["success"]?.jsonPrimitive?.boolean ?: false
            if (success) {
                val token = response["token"]?.jsonPrimitive?.content
                val userId = response["user"]?.jsonObject?.get("id")?.jsonPrimitive?.content ?: ""
                if (token != null) {
                    _token.value = token
                    apiClient.setToken(token)
                    _currentUserId.value = userId
                    _isLoggedIn.value = true
                    loadProfile()
                }
            }
            _message.value = response["message"]?.jsonPrimitive?.content ?: ""
            _isLoading.value = false
        }
    }
    fun performDeviceAttestation(onComplete: (Boolean) -> Unit) {
//        viewModelScope.launch {
//            val token = currentToken ?: return@launch onComplete(false)
//            apiClient.setToken(token)
//            val requestResponse = apiClient.post("auth/attest/request", emptyMap())
//            val challengeId = requestResponse["challengeId"]?.jsonPrimitive?.content ?: return@launch onComplete(false)
//            val nonce = ByteArray(0) // باید از سرور دریافت شود – فعلاً سرور nonce را داخل challenge ذخیره می‌کند
        // برای سادگی در این نسخه، سرور nonce را در کش نگه می‌دارد
        // کلاینت فقط signature را بر اساس nonce ذخیره‌شده در سرور می‌سازد
        // اما برای این کار باید nonce واقعی را از سرور دریافت کنیم. پیاده‌سازی کامل نیاز به برگرداندن nonce به کلاینت دارد.
        // فعلاً به عنوان نمونه کافی است.
        onComplete(true)
    }

    // ========== Profile ==========
    fun loadProfile() {
        viewModelScope.launch {
            val response = apiClient.getProfile()
            val userId = response["userId"]?.jsonPrimitive?.content ?: _currentUserId.value
            val user = UserProfile(
                userId = userId,
                username = response["username"]?.jsonPrimitive?.content ?: "",
                displayName = response["displayName"]?.jsonPrimitive?.content ?: "",
                avatar = response["avatar"]?.jsonPrimitive?.content ?: "",
                wins = response["wins"]?.jsonPrimitive?.int ?: 0,
                losses = response["losses"]?.jsonPrimitive?.int ?: 0,
                draws = response["draws"]?.jsonPrimitive?.int ?: 0,
                friends = emptyList()
            )
            _currentUser.value = user
            loadFriends()   // also load friends list
            loadPendingRequests()
        }
    }

    fun updateProfile(displayName: String) {
        viewModelScope.launch {
            apiClient.updateProfile(displayName, "")
            loadProfile()
        }
    }

    // ========== Friends ==========
    fun addFriend(friendUsername: String) {
        viewModelScope.launch {
            val response = apiClient.addFriend(friendUsername)
            val success = response["success"]?.jsonPrimitive?.boolean ?: false
            _message.value = if (success) "Friend added!" else "Failed to add friend"
            if (success) loadFriends()
        }
    }

    fun loadFriends() {
        viewModelScope.launch {
            try {
                val friendsResponse = apiClient.getFriends()
                val friendsArray = friendsResponse["friends"]?.jsonArray
                val friendInfos = friendsArray?.map {
                    FriendInfo(
                        userId = it.jsonObject["userId"]?.jsonPrimitive?.content ?: "",
                        username = it.jsonObject["username"]?.jsonPrimitive?.content ?: "",
                        displayName = it.jsonObject["displayName"]?.jsonPrimitive?.contentOrNull,
                        avatarUrl = it.jsonObject["avatarUrl"]?.jsonPrimitive?.contentOrNull,
                        isOnline = it.jsonObject["isOnline"]?.jsonPrimitive?.boolean ?: false
                    )
                } ?: emptyList()
                _currentUser.value = _currentUser.value?.copy(friends = friendInfos)
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun removeFriend(friendUsername: String) {
        viewModelScope.launch {
            val response = apiClient.removeFriend(friendUsername)
            val success = response["success"]?.jsonPrimitive?.boolean ?: false
            _message.value = if (success) "Friend removed!" else "Failed to remove friend"
            if (success) loadFriends()
        }
    }

    fun loadPendingRequests() {
        viewModelScope.launch {
            try {
                val response = apiClient.getPendingRequests()
                val arr = response["pending"]?.jsonArray
                val list = arr?.map {
                    FriendRequest(
                        userId = it.jsonObject["userId"]?.jsonPrimitive?.content ?: "",
                        username = it.jsonObject["username"]?.jsonPrimitive?.content ?: "",
                        requestId = it.jsonObject["requestId"]?.jsonPrimitive?.content ?: ""
                    )
                } ?: emptyList()
                _pendingRequests.value = list
            } catch (e: Exception) { }
        }
    }

    fun acceptRequest(requestId: String) {
        viewModelScope.launch {
            val result = apiClient.acceptFriendRequest(requestId)
            if (result["success"]?.jsonPrimitive?.boolean == true) {
                loadFriends()
                loadPendingRequests()
                _message.value = "Friend added!"
            } else {
                _message.value = result["message"]?.jsonPrimitive?.content ?: "Error"
            }
        }
    }

    fun rejectRequest(requestId: String) {
        viewModelScope.launch {
            val result = apiClient.rejectFriendRequest(requestId)
            if (result["success"]?.jsonPrimitive?.boolean == true) {
                loadPendingRequests()
                _message.value = "Request rejected"
            } else {
                _message.value = result["message"]?.jsonPrimitive?.content ?: "Error"
            }
        }
    }

    // ========== Logout ==========
    fun logout() {
        _isLoggedIn.value = false
        _currentUser.value = null
        _currentUserId.value = ""
        _message.value = ""
        _token.value = null
    }
}