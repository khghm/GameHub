package com.gamehub.host.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamehub.host.network.Society
import com.gamehub.host.network.SocietyClient
import com.gamehub.host.network.SocietyMember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SocietyViewModel : ViewModel() {
    private val client = SocietyClient()
    private val _societies = MutableStateFlow<List<Society>>(emptyList())
    val societies: StateFlow<List<Society>> = _societies
    private val _currentSociety = MutableStateFlow<Society?>(null)
    val currentSociety: StateFlow<Society?> = _currentSociety
    private val _members = MutableStateFlow<List<SocietyMember>>(emptyList())
    val members: StateFlow<List<SocietyMember>> = _members
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun loadAllSocieties() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _societies.value = client.getAllSocieties()
            } catch (e: Exception) {
                _message.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadSociety(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _currentSociety.value = client.getSociety(id)
                if (_currentSociety.value != null) {
                    loadMembers(id)
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

    private fun loadMembers(societyId: String) {
        viewModelScope.launch {
            _members.value = client.getSocietyMembers(societyId)
        }
    }

    fun createSociety(name: String, description: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = client.createSociety(name, description)
                if (result != null && result.success) {
                    _message.value = result.message
                    loadAllSocieties()
                } else {
                    _message.value = result?.message ?: "خطا در ایجاد انجمن"
                }
            } catch (e: Exception) {
                _message.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun joinSociety(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = client.joinSociety(id)
                if (result != null && result.success) {
                    _message.value = result.message
                    loadSociety(id)
                    loadAllSocieties()
                } else {
                    _message.value = result?.message ?: "خطا در عضویت"
                }
            } catch (e: Exception) {
                _message.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun leaveSociety(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = client.leaveSociety(id)
                if (result != null && result.success) {
                    _message.value = result.message
                    if (_currentSociety.value?.id == id) {
                        _currentSociety.value = null
                        _members.value = emptyList()
                    }
                    loadAllSocieties()
                } else {
                    _message.value = result?.message ?: "خطا در خروج"
                }
            } catch (e: Exception) {
                _message.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun approveMember(userId: String) {
        val society = _currentSociety.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = client.approveMember(society.id, userId)
                if (result != null && result.success) {
                    _message.value = result.message
                    loadSociety(society.id)
                } else {
                    _message.value = result?.message ?: "خطا در تایید عضویت"
                }
            } catch (e: Exception) {
                _message.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun rejectMember(userId: String) {
        val society = _currentSociety.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = client.rejectMember(society.id, userId)
                if (result != null && result.success) {
                    _message.value = result.message
                    loadSociety(society.id)
                } else {
                    _message.value = result?.message ?: "خطا در رد عضویت"
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
