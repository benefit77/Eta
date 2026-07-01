package fuck.andes.ui.model

sealed interface AgentHomeAction {
    data class InputChanged(val text: String) : AgentHomeAction
    data class ThinkingToggled(val enabled: Boolean) : AgentHomeAction
    data object SendMessage : AgentHomeAction
    data object StopRun : AgentHomeAction
    data class ImageAttached(val uri: String) : AgentHomeAction
    data class RemoveImage(val id: String) : AgentHomeAction
    data object OpenTools : AgentHomeAction
    data object OpenPermissions : AgentHomeAction
    data object OpenSystemEnhance : AgentHomeAction
    data object OpenSettings : AgentHomeAction
    data object ExpandRunTrace : AgentHomeAction
}

sealed interface PermissionHealthAction {
    data class OpenItemAction(val itemId: String) : PermissionHealthAction
    data object NavigateBack : PermissionHealthAction
}

sealed interface AgentChatAction {
    data object NavigateBack : AgentChatAction
    data class InputChanged(val text: String) : AgentChatAction
    data class ThinkingToggled(val enabled: Boolean) : AgentChatAction
    data object SendMessage : AgentChatAction
    data object StopRun : AgentChatAction
    data class ImageAttached(val uri: String) : AgentChatAction
    data class RemoveImage(val id: String) : AgentChatAction
}

sealed interface AgentToolsAction {
    data object NavigateBack : AgentToolsAction
}

sealed interface AgentSystemEnhanceAction {
    data object NavigateBack : AgentSystemEnhanceAction
    data class ToggleItem(val itemId: String) : AgentSystemEnhanceAction
}
