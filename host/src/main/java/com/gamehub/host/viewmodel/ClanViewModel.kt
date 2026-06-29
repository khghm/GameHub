package com.gamehub.host.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamehub.host.network.Clan
import com.gamehub.host.network.ClanClient
import com.gamehub.host.network.ClanMember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ClanViewModel : ViewModel() {
    private val client = ClanClient()
    private val _myClan = MutableStateFlow<Clan?>(null)
    val myClan: StateFlow<Clan?> = _myClan
    private val _members = MutableStateFlow<List<ClanMember>>(emptyList())
    val members: StateFlow<List<ClanMember>> = _members
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun loadMyClan() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val clan = client.getMyClan()
                _myClan.value = clan
                if (clan != null) {
                    loadMembers(clan.id)
                } else {
                    _members.value = emptyList()
                }
            } catch (e: Exception) {
                _message.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadMembers(clanId: String) {
        viewModelScope.launch {
            _members.value = client.getClanMembers(clanId)
        }
    }

    fun createClan(name: String, tag: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = client.createClan(name, tag)
                if (result != null && result.success) {
                    _message.value = result.message
                    loadMyClan()
                } else {
                    _message.value = result?.message ?: "خطا در ایجاد کلن"
                }
            } catch (e: Exception) {
                _message.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun joinClan(clanId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = client.joinClan(clanId)
                if (result != null && result.success) {
                    _message.value = result.message
                    loadMyClan()
                } else {
                    _message.value = result?.message ?: "خطا در پیوستن به کلن"
                }
            } catch (e: Exception) {
                _message.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun leaveClan() {
        val clan = _myClan.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = client.leaveClan(clan.id)
                if (result != null && result.success) {
                    _message.value = result.message
                    _myClan.value = null
                    _members.value = emptyList()
                } else {
                    _message.value = result?.message ?: "خطا در خروج از کلن"
                }
            } catch (e: Exception) {
                _message.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun upgradeClan() {
        val clan = _myClan.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = client.upgradeClan(clan.id)
                if (result != null && result.success) {
                    _message.value = result.message
                    loadMyClan()
                } else {
                    _message.value = result?.message ?: "ارتقا ممکن نیست (سکه کافی یا حداکثر سطح)"
                }
            } catch (e: Exception) {
                _message.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun contributeCoins(amount: Long) {
        val clan = _myClan.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = client.contributeCoins(clan.id, amount)
                if (result != null && result.success) {
                    _message.value = result.message
                    loadMyClan()
                } else {
                    _message.value = result?.message ?: "خطا در کمک سکه (موجودی کافی نیست)"
                }
            } catch (e: Exception) {
                _message.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}