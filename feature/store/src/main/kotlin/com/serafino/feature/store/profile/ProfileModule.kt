package com.serafino.feature.store.profile

import com.serafino.architecture.AnalyticsTracking
import com.serafino.architecture.AppRoute
import com.serafino.architecture.AuthProviding
import com.serafino.architecture.DependencyContainer
import com.serafino.architecture.EventBus
import com.serafino.architecture.NoOpAnalytics
import com.serafino.architecture.NoOpAuthService
import com.serafino.architecture.Presenter
import com.serafino.architecture.Router
import com.serafino.architecture.resolve
import com.serafino.architecture.require
import com.serafino.domain.services.AccountService
import com.serafino.domain.services.LoyaltyProviding

/** Presenter del Perfil. Espeja `ProfilePresenter` de iOS. */
class ProfilePresenter(
    override val interactor: ProfileInteractor,
    private val router: Router,
) : Presenter<ProfileInteractor> {
    fun onAppear() = interactor.handle(ProfileInteractor.Input.OnAppear)

    /** Pull-to-refresh: espera la recarga para que el control nativo mantenga el spinner. */
    suspend fun refresh() = interactor.refresh()

    /** Mantiene la cuenta al día mientras el Perfil está visible (auto-refresco periódico). */
    suspend fun autoRefresh() = interactor.autoRefresh()

    fun openRedeem() = router.push(AppRoute.LoyaltyRedeem)
    fun signIn() = interactor.handle(ProfileInteractor.Input.SignIn)
    fun signOut() = interactor.handle(ProfileInteractor.Input.SignOut)
    fun retry() = interactor.handle(ProfileInteractor.Input.Retry)

    fun requestDeleteAccount() = interactor.handle(ProfileInteractor.Input.DeleteAccountRequested)
    fun confirmDeleteAccount() = interactor.handle(ProfileInteractor.Input.DeleteAccountConfirmed)
    fun cancelDeleteAccount() = interactor.handle(ProfileInteractor.Input.DeleteAccountCancelled)
}

/** Ensambla el stack VIPER del Perfil. Espeja `ProfileModule` de iOS. */
object ProfileModule {
    fun build(container: DependencyContainer, router: Router): ProfilePresenter {
        val interactor = ProfileInteractor(
            auth = container.resolve<AuthProviding>() ?: NoOpAuthService(),
            loyalty = container.require<LoyaltyProviding>(),
            bus = container.require<EventBus>(),
            analytics = container.resolve<AnalyticsTracking>() ?: NoOpAnalytics(),
            account = container.resolve<AccountService>(),
        )
        return ProfilePresenter(interactor, router)
    }
}
