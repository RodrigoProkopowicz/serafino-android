package com.serafino.domain.services

/**
 * Borrado de la cuenta del usuario autenticado. Google Play (y App Store) exigen que una app con
 * creación de cuenta permita eliminarla desde la propia app; este servicio ejecuta ese borrado
 * contra el backend.
 *
 * El backend borra la cuenta de fidelidad completa (granos, historial y vales), desvincula los
 * pedidos históricos de la identidad (los conserva por obligaciones contables) y borra el usuario
 * del proveedor de auth. La app confirma la decisión y advierte la pérdida de granos y vales ANTES
 * de llamar. El borrado es idempotente: reintentar tras un fallo parcial vuelve a devolver éxito.
 */
interface AccountService {
    /**
     * Elimina la cuenta del usuario autenticado. La implementación adjunta el ID token del usuario
     * como prueba de identidad. Lanza si la request falla (el llamador debe informar el error y NO
     * cerrar la sesión local, para permitir el reintento).
     */
    suspend fun deleteAccount()
}
