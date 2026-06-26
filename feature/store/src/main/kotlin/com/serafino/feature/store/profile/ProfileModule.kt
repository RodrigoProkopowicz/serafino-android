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
import com.serafino.domain.services.LoyaltyProviding

/** Presenter del Perfil. Espeja `ProfilePresenter` de iOS. */
class ProfilePresenter(
    override val interactor: ProfileInteractor,
    private val router: Router,
) : Presenter<ProfileInteractor> {
    fun onAppear() = interactor.handle(ProfileInteractor.Input.OnAppear)
    fun openRedeem() = router.push(AppRoute.LoyaltyRedeem)
    fun signIn() = interactor.handle(ProfileInteractor.Input.SignIn)
    fun signOut() = interactor.handle(ProfileInteractor.Input.SignOut)
    fun retry() = interactor.handle(ProfileInteractor.Input.Retry)
}

/** Ensambla el stack VIPER del Perfil. Espeja `ProfileModule` de iOS. */
object ProfileModule {
    fun build(container: DependencyContainer, router: Router): ProfilePresenter {
        val interactor = ProfileInteractor(
            auth = container.resolve<AuthProviding>() ?: NoOpAuthService(),
            loyalty = container.require<LoyaltyProviding>(),
            bus = container.require<EventBus>(),
            analytics = container.resolve<AnalyticsTracking>() ?: NoOpAnalytics(),
        )
        return ProfilePresenter(interactor, router)
    }
}
