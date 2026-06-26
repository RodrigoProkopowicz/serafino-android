package com.serafino.data.catalog

import com.serafino.domain.entities.BrewMethod
import com.serafino.domain.entities.BrewStep
import com.serafino.domain.entities.Difficulty
import com.serafino.domain.entities.GlossaryTerm
import com.serafino.domain.entities.Recipe
import com.serafino.domain.services.RecipeCatalogProviding

/**
 * Recetas curadas y ajustadas a mano (espresso 1:2, vertido 1:16–1:17). Cada receta está pensada
 * para alguien que recién empieza. Espeja `StaticRecipeCatalog` de iOS.
 */
class StaticRecipeCatalog : RecipeCatalogProviding {
    private val recipes: List<Recipe> = makeRecipes()

    override fun allRecipes(): List<Recipe> = recipes
    override fun recipe(id: String): Recipe? = recipes.firstOrNull { it.id == id }
    override fun recipes(method: BrewMethod): List<Recipe> = recipes.filter { it.method == method }

    private companion object {
        // Palabras que aparecen en varias recetas. Se definen una vez y se reutilizan.
        val ratioTerm = GlossaryTerm(
            term = "Ratio (proporción)",
            definition = "La relación entre café y agua. \"1:2\" quiere decir el doble de agua que de café; \"1:16\", dieciséis veces más agua que café. Cuanta más agua, más suave la taza.",
        )
        val grindTerm = GlossaryTerm(
            term = "Molienda",
            definition = "Qué tan fino o grueso queda el café después de molerlo. Cambia cuánto sabor suelta: muy fino tiende a amargo, muy grueso queda aguado.",
        )
        val extractionTerm = GlossaryTerm(
            term = "Extracción",
            definition = "Es el agua \"sacándole\" el sabor al café. Si saca de menos, queda ácido y flojo; si saca de más, queda amargo. El objetivo es el punto justo.",
        )

        fun makeRecipes(): List<Recipe> = listOf(
            espressoClassic, flatWhite, v60Hario, aeropressStandard,
            chemexClassic, frenchPressClassic, mokaStovetop, coldBrewConcentrate,
        )

        val espressoClassic = Recipe(
            id = "espresso-classic",
            name = "Espresso Clásico",
            method = BrewMethod.Espresso,
            summary = "Un doble equilibrado, extraído en ratio 1:2.",
            difficulty = Difficulty.Medium,
            coffeeGrams = 18.0,
            waterGrams = 36.0,
            grind = "Fina, como azúcar impalpable",
            waterTempC = 93,
            brewTimeText = "25–30 s",
            steps = listOf(
                BrewStep(1, "Dosificá y distribuí", "Molé 18 g frescos en el portafiltro y nivelá la cama de café.", manualHint = "Nivelada", why = "Si la capa de café queda pareja, el agua pasa igual por todos lados. Si queda despareja, una parte sale amarga y otra floja."),
                BrewStep(2, "Tampeá", "Tampeá firme y parejo, después limpiá los restos del borde.", manualHint = "Nivelado y firme", why = "Apretar parejo evita que el agua encuentre huecos fáciles por donde escaparse sin llevarse sabor."),
                BrewStep(3, "Extraé el shot", "Colocá el portafiltro y arrancá la bomba. Apuntá a 36 g en la taza.", duration = 28, waterTarget = 36, why = "El peso final y el tiempo son tu termómetro: te dicen si la molienda quedó bien para ajustar la próxima."),
            ),
            flavorNotes = listOf("Caramelo", "Chocolate amargo", "Almibarado"),
            tips = listOf(
                "Apuntá a un ratio 1:2 (18 g dentro, 36 g afuera) en 25–30 segundos.",
                "¿Sale muy rápido? Molé más fino. ¿Se traba la máquina? Molé más grueso.",
            ),
            beginnerIntro = "El espresso es un café chiquito pero intenso: la máquina empuja agua caliente a presión a través de café bien molido y sale un \"shot\" espeso y aromático, con una capa color caramelo arriba (la crema). Es la base de casi todos los cafés con leche. Requiere algo de práctica con la máquina, pero esta receta te da los números para arrancar con confianza.",
            equipment = listOf(
                "Máquina de espresso",
                "Molinillo (la molienda fina es clave acá)",
                "Balanza de cocina (ideal si mide 0,1 g)",
                "18 g de café en grano fresco",
                "Tamper (el disco para apretar el café)",
            ),
            glossary = listOf(
                GlossaryTerm("Shot", "Cada porción de espresso que sale de la máquina. Un \"doble\" son dos juntas (lo más común)."),
                GlossaryTerm("Portafiltro", "El mango con un canasto de metal donde va el café molido; se engancha a la máquina."),
                GlossaryTerm("Tampear", "Apretar el café molido dentro del portafiltro con el tamper para dejar una base pareja y firme."),
                GlossaryTerm("Crema", "La capa de espuma color caramelo que se forma arriba de un espresso bien hecho."),
                ratioTerm, grindTerm,
            ),
        )

        val flatWhite = Recipe(
            id = "flat-white",
            name = "Flat White",
            method = BrewMethod.Espresso,
            summary = "Un doble ristretto bajo una microespuma sedosa.",
            difficulty = Difficulty.Medium,
            coffeeGrams = 18.0,
            waterGrams = 36.0,
            grind = "Fina, como azúcar impalpable",
            waterTempC = 93,
            brewTimeText = "≈3 min",
            steps = listOf(
                BrewStep(1, "Extraé un doble", "Extraé 36 g de espresso directo en la taza.", duration = 28, waterTarget = 36, why = "Es la base de sabor: querés que salga dulce y equilibrado antes de sumarle la leche."),
                BrewStep(2, "Espumá la leche", "Espumá ~120 ml de leche hasta una microespuma brillante, cerca de 60 °C.", manualHint = "Microespuma sedosa", why = "La microespuma es lo que da la textura cremosa. Burbujas grandes dan una espuma seca tipo capuchino, no lo que buscamos."),
                BrewStep(3, "Serví", "Verté desde abajo y después levantá para dejar una capa fina de espuma.", manualHint = "Bajo y después alto", why = "Verter desde abajo mezcla la leche con el café; levantar al final deja la capa prolija de espuma arriba."),
            ),
            flavorNotes = listOf("Aterciopelado", "Leche dulce", "Espresso al frente"),
            tips = listOf(
                "La microespuma tiene que verse como pintura húmeda brillante, sin burbujas grandes.",
                "La leche entera es la más fácil de espumar.",
            ),
            beginnerIntro = "Un flat white es un espresso con leche cremosa por encima: suave y dulce, pero con bastante sabor a café. El truco está en calentar la leche con vapor hasta que quede sedosa (sin burbujas grandes) y combinarla con el espresso. Si te gusta el café con leche pero querés algo más \"cafetero\" que un latte, este es para vos.",
            equipment = listOf(
                "Máquina de espresso con vaporizador de leche",
                "Jarra de metal para espumar",
                "120 ml de leche bien fría (la entera es la más fácil)",
                "18 g de café en grano",
                "Una taza de unos 150–180 ml",
            ),
            glossary = listOf(
                GlossaryTerm("Espumar la leche", "Calentar la leche con el vapor de la máquina hasta dejarla cremosa y brillante."),
                GlossaryTerm("Microespuma", "Leche espumada con burbujas tan chiquitas que casi no se ven; queda lisa como pintura húmeda."),
                GlossaryTerm("Ristretto", "Un espresso \"corto\": usa un poco menos de agua, así queda más concentrado y dulce."),
                ratioTerm,
            ),
        )

        val v60Hario = Recipe(
            id = "v60-hario",
            name = "Hario V60",
            method = BrewMethod.PourOverV60,
            summary = "Una taza individual, brillante y limpia, vertido a vertido.",
            difficulty = Difficulty.Medium,
            coffeeGrams = 15.0,
            waterGrams = 250.0,
            grind = "Media-fina, como sal de mesa",
            waterTempC = 94,
            brewTimeText = "2:45",
            steps = listOf(
                BrewStep(1, "Enjuagá y prepará", "Enjuagá el filtro de papel con agua caliente, descartala y agregá 15 g de café.", manualHint = "Enjuagar filtro", why = "El agua caliente saca el gusto a papel del filtro y entibia todo, para que la temperatura no se desplome al verter."),
                BrewStep(2, "Pre-infusión", "Verté 45 g para mojar todo el café, hacé un movimiento circular suave y esperá.", duration = 45, waterTarget = 45, why = "El café fresco larga gas (lo vas a ver hincharse y burbujear). Dejarlo escapar primero hace que después el agua extraiga parejo."),
                BrewStep(3, "Primer vertido", "Verté en círculos lentos hasta llegar a 150 g.", duration = 30, waterTarget = 150, why = "Verter de a poco y en círculos moja todo el café por igual, sin dejar zonas secas."),
                BrewStep(4, "Vertido final", "Completá hasta 250 g manteniendo la cama plana.", duration = 30, waterTarget = 250, why = "Si la cama queda plana, el agua no se escapa por un costado (canalización) dejando café sin extraer."),
                BrewStep(5, "Drenado", "Dejá que filtre. Apuntá a un tiempo total cercano a 2:45.", duration = 60, waterTarget = 250, why = "El tiempo total te avisa si la molienda está bien: muy lento, molé más grueso; muy rápido, más fino."),
            ),
            flavorNotes = listOf("Floral", "Cítrico", "Tipo té"),
            tips = listOf(
                "Mantené un vertido central y constante para una extracción pareja.",
                "¿Filtra muy lento? Molé más grueso. ¿Muy rápido y aguado? Molé más fino.",
            ),
            beginnerIntro = "El V60 es un café \"de filtro\" que preparás a mano: vas vertiendo agua caliente de a poco sobre el café apoyado en un filtro de papel con forma de cono. El resultado es una taza limpia, suave y aromática, muy distinta del espresso. No necesitás máquina, solo paciencia y un pulso tranquilo. Es ideal para descubrir los sabores escondidos de un café.",
            equipment = listOf(
                "Cono V60 con filtros de papel",
                "Balanza de cocina con cronómetro",
                "Pava, mejor de pico fino (\"cuello de cisne\")",
                "Molinillo",
                "15 g de café · 250 g de agua",
            ),
            glossary = listOf(
                GlossaryTerm("Vertido (pour over)", "Método donde vos mismo vertés el agua caliente, de a poco, sobre el café apoyado en un filtro."),
                GlossaryTerm("Pre-infusión / florecer", "El primer chorrito moja todo el café y lo deja \"respirar\" unos segundos: largará gas y después extrae más parejo."),
                GlossaryTerm("Canalización", "Cuando el agua encuentra un atajo por la cama de café en vez de pasar pareja. Da una taza desbalanceada."),
                ratioTerm, grindTerm,
            ),
        )

        val aeropressStandard = Recipe(
            id = "aeropress-standard",
            name = "AeroPress",
            method = BrewMethod.Aeropress,
            summary = "Rápida, suave y muy indulgente — ideal para cualquier grano.",
            difficulty = Difficulty.Easy,
            coffeeGrams = 15.0,
            waterGrams = 230.0,
            grind = "Media-fina",
            waterTempC = 85,
            brewTimeText = "1:40",
            steps = listOf(
                BrewStep(1, "Preparalo todo", "Poné un filtro enjuagado, agregá 15 g de café y apoyala sobre una taza firme.", manualHint = "Enjuagar y dosificar", why = "Enjuagar el filtro saca el gusto a papel; tener todo listo evita que el café se enfríe a mitad de camino."),
                BrewStep(2, "Verté y revolvé", "Agregá 230 g de agua, revolvé tres veces y colocá el émbolo para sellar.", duration = 15, waterTarget = 230, why = "Revolver asegura que todo el café se moje; si queda seco, esa parte no aporta nada de sabor."),
                BrewStep(3, "Reposo", "Dejá reposar sin tocar.", duration = 60, waterTarget = 230, why = "Es el rato en que el agua le saca el sabor al café. Más reposo da una taza más fuerte."),
                BrewStep(4, "Presioná", "Presioná suave y pará cuando escuches el siseo.", duration = 25, waterTarget = 230, why = "Presionar despacio evita amargor; el siseo avisa que ya pasó todo el café y conviene frenar."),
            ),
            flavorNotes = listOf("Suave", "Chocolate", "Equilibrado"),
            tips = listOf(
                "Bajar la temperatura del agua (80–85 °C) mantiene a raya el amargor.",
                "Probá el método invertido para reposar más tiempo sin que gotee.",
            ),
            beginnerIntro = "La AeroPress es probablemente la forma más fácil y a prueba de errores de hacer un café rico sin máquina. Ponés café y agua en un tubo, esperás un ratito y empujás con un émbolo (como una jeringa grande). Sale un café suave y parejo en menos de dos minutos. Perfecta para dar tus primeros pasos sin frustrarte.",
            equipment = listOf(
                "AeroPress con un filtro de papel",
                "Balanza o una cuchara medidora",
                "Agua caliente, no hirviendo (80–85 °C)",
                "15 g de café molido medio",
                "Una taza resistente al calor",
            ),
            glossary = listOf(
                GlossaryTerm("Émbolo", "La parte que empuja, como en una jeringa, para hacer pasar el café por el filtro."),
                GlossaryTerm("Método invertido", "Armar la AeroPress al revés para que el café repose más tiempo sin gotear, y darla vuelta para presionar."),
                grindTerm, extractionTerm,
            ),
        )

        val chemexClassic = Recipe(
            id = "chemex-classic",
            name = "Chemex",
            method = BrewMethod.Chemex,
            summary = "Cristalina y tipo té, gracias a su filtro de papel grueso.",
            difficulty = Difficulty.Medium,
            coffeeGrams = 30.0,
            waterGrams = 500.0,
            grind = "Media-gruesa",
            waterTempC = 94,
            brewTimeText = "4:00",
            steps = listOf(
                BrewStep(1, "Enjuagá el filtro", "Acomodá el filtro grueso con las tres capas hacia el pico, enjuagá y descartá el agua.", manualHint = "Enjuagar filtro", why = "El filtro de la Chemex es grueso y sabe mucho a papel si no lo enjuagás. De paso entibia la jarra."),
                BrewStep(2, "Pre-infusión", "Agregá 30 g de café, verté 80 g y dejá que florezca.", duration = 45, waterTarget = 80, why = "Igual que en el V60: dejás que el café fresco largue su gas para que después extraiga parejo."),
                BrewStep(3, "Vertidos por etapas", "Verté en etapas lentas hasta 350 g.", duration = 120, waterTarget = 350, why = "Agregar el agua en tandas mantiene la temperatura y la extracción estables de principio a fin."),
                BrewStep(4, "Vertido final", "Completá hasta 500 g manteniendo la cama nivelada.", duration = 30, waterTarget = 500, why = "Una cama pareja evita que el agua se escape por un costado sin llevarse sabor."),
                BrewStep(5, "Drenado", "Dejá que termine cerca de los 4:00.", duration = 75, waterTarget = 500, why = "Si tarda bastante más de 4 minutos, la próxima vez molé un poco más grueso."),
            ),
            flavorNotes = listOf("Limpia", "Brillante", "Crujiente"),
            tips = listOf(
                "El filtro grueso da una taza excepcionalmente limpia.",
                "Mantené la cama plana para evitar canalizaciones.",
            ),
            beginnerIntro = "La Chemex es una jarra de vidrio elegante que usa un filtro de papel grueso. Vertés el agua a mano, parecido al V60, pero el filtro retiene más aceites y da una taza muy limpia y delicada. Es como preparar un café \"de filtro\" grande para compartir con alguien.",
            equipment = listOf(
                "Jarra Chemex con sus filtros gruesos",
                "Balanza de cocina con cronómetro",
                "Pava de pico fino",
                "Molinillo",
                "30 g de café · 500 g de agua",
            ),
            glossary = listOf(
                GlossaryTerm("Vertido (pour over)", "Vos mismo vertés el agua caliente, de a poco, sobre el café apoyado en el filtro."),
                GlossaryTerm("Pre-infusión / florecer", "El primer chorrito moja el café y lo deja largar gas unos segundos antes de seguir."),
                GlossaryTerm("Canalización", "Cuando el agua se abre un atajo en la cama de café y pasa sin extraer parejo."),
                ratioTerm, grindTerm,
            ),
        )

        val frenchPressClassic = Recipe(
            id = "frenchpress-classic",
            name = "Prensa Francesa",
            method = BrewMethod.FrenchPress,
            summary = "Inmersión total con mucho cuerpo y casi sin esfuerzo.",
            difficulty = Difficulty.Easy,
            coffeeGrams = 30.0,
            waterGrams = 500.0,
            grind = "Gruesa, como sal marina",
            waterTempC = 95,
            brewTimeText = "4:00",
            steps = listOf(
                BrewStep(1, "Agregá el café", "Agregá 30 g de café grueso a la jarra.", manualHint = "Dosificar 30 g", why = "La molienda gruesa es clave: si es fina, se cuela por el filtro de metal y la taza queda barrosa."),
                BrewStep(2, "Verté y pre-infusión", "Verté 100 g, revolvé la costra para romperla y dejá que florezca.", duration = 30, waterTarget = 100, why = "Al verter, el café flota y forma una costra arriba. Revolverla hace que todo el café toque el agua."),
                BrewStep(3, "Completá", "Llená hasta 500 g y apoyá la tapa arriba — todavía no bajes el émbolo.", duration = 30, waterTarget = 500, why = "Llenar y tapar mantiene el calor mientras el café se hace en remojo."),
                BrewStep(4, "Reposo", "Dejá reposar cuatro minutos.", duration = 240, waterTarget = 500, why = "En la prensa el café está en remojo todo el tiempo; cuatro minutos es el punto justo de sabor."),
                BrewStep(5, "Espumá y prensá", "Sacá la espuma de la superficie y después bajá el émbolo despacio.", manualHint = "Prensado lento", why = "Bajar el émbolo despacio evita revolver el poso del fondo y que se cuele a la taza."),
            ),
            flavorNotes = listOf("Con cuerpo", "Cacao", "Redonda"),
            tips = listOf(
                "Servila apenas la prensás para que no se sobre-extraiga.",
                "Una molienda gruesa y pareja evita que quede barrosa.",
            ),
            beginnerIntro = "La prensa francesa es de las formas más simples de hacer café: ponés café grueso y agua caliente, esperás cuatro minutos y bajás un filtro de metal con el émbolo. No hay que verter con técnica ni medir tiempos exactos. Da un café con cuerpo, redondo y sabroso. Ideal si recién arrancás y querés algo difícil de arruinar.",
            equipment = listOf(
                "Una prensa francesa",
                "Balanza o cuchara medidora",
                "Pava o agua recién hervida",
                "30 g de café molido grueso",
                "Una cuchara para revolver",
            ),
            glossary = listOf(
                GlossaryTerm("Inmersión", "El café queda en remojo dentro del agua todo el tiempo, como un té, en vez de filtrarse de a poco."),
                GlossaryTerm("Costra", "La capa de café que flota arriba al verter el agua. Romperla con la cuchara reparte el sabor."),
                GlossaryTerm("Émbolo", "El pistón con malla de metal que bajás para separar el café del líquido."),
                grindTerm,
            ),
        )

        val mokaStovetop = Recipe(
            id = "moka-stovetop",
            name = "Cafetera Moka",
            method = BrewMethod.MokaPot,
            summary = "Café de hornalla intenso y audaz, con una capa tipo crema.",
            difficulty = Difficulty.Medium,
            coffeeGrams = 17.0,
            waterGrams = 150.0,
            grind = "Fina-media",
            waterTempC = 100,
            brewTimeText = "≈4 min",
            steps = listOf(
                BrewStep(1, "Llená la base", "Llená la cámara inferior con agua caliente hasta la válvula.", manualHint = "Agua caliente a la válvula", why = "Empezar con agua caliente hace que el café se haga rápido y no quede con gusto cocido o metálico."),
                BrewStep(2, "Agregá el café", "Llená el embudo con café y nivelalo — nunca lo tampees.", manualHint = "Nivelado, sin tampear", why = "Si lo apretás, la presión sube de más y el café sale quemado y amargo. Solo nivelalo."),
                BrewStep(3, "Calentá", "Armala y ponela a fuego medio con la tapa abierta.", manualHint = "Fuego medio", why = "Fuego medio y tapa abierta te dejan ver cuándo está listo y evitan que se recaliente."),
                BrewStep(4, "Sacala del fuego", "Cuando gorgotee y el chorro se ponga claro, sacala y enfriá la base.", manualHint = "Al primer gorgoteo", why = "El gorgoteo avisa que ya casi no queda agua abajo; seguir en el fuego solo agrega amargor."),
            ),
            flavorNotes = listOf("Intenso", "Agridulce", "Audaz"),
            tips = listOf(
                "Empezar con agua caliente evita un sabor cocido y metálico.",
                "Sacala del fuego temprano — el gorgoteo significa que ya casi está.",
            ),
            beginnerIntro = "La cafetera moka (esa de aluminio con forma de reloj de arena) hace un café fuerte y concentrado sobre la hornalla. El agua hierve en la base y sube a presión a través del café hasta la parte de arriba. No es un espresso de verdad, pero se le acerca y es muy popular para casa. El secreto es no pasarse de fuego.",
            equipment = listOf(
                "Cafetera moka (del tamaño que tengas)",
                "Molinillo (molienda fina-media)",
                "17 g de café, o lo que entre en el embudo sin apretar",
                "Agua caliente",
                "Una hornalla",
            ),
            glossary = listOf(
                GlossaryTerm("Válvula", "La perilla de seguridad en la base. Nunca llenes el agua por encima de ella."),
                GlossaryTerm("Embudo", "El canastito de metal, con forma de embudo, donde va el café molido."),
                GlossaryTerm("Gorgoteo", "El burbujeo ruidoso del final: avisa que ya casi no queda agua abajo y hay que sacarla del fuego."),
                grindTerm,
            ),
        )

        val coldBrewConcentrate = Recipe(
            id = "coldbrew-concentrate",
            name = "Cold Brew",
            method = BrewMethod.ColdBrew,
            summary = "Concentrado suave, dulce y de baja acidez, hecho de un día para el otro.",
            difficulty = Difficulty.Easy,
            coffeeGrams = 80.0,
            waterGrams = 1000.0,
            grind = "Extra gruesa",
            waterTempC = 20,
            brewTimeText = "12–18 h",
            steps = listOf(
                BrewStep(1, "Combiná", "Agregá 80 g de café grueso y 1000 g de agua fría. Revolvé para saturar.", manualHint = "Revolver para saturar", why = "Revolver moja todo el café; si queda polvo seco flotando, esa parte no aporta sabor."),
                BrewStep(2, "Reposo", "Tapá y dejá reposar en la heladera.", manualHint = "12–18 h", why = "El agua fría saca el sabor muy de a poco; por eso necesita horas en vez de minutos."),
                BrewStep(3, "Filtrá", "Filtrá con un papel. El concentrado dura alrededor de una semana.", manualHint = "Filtrar y guardar", why = "Sacar todo el poso detiene la extracción y deja el concentrado limpio y guardable."),
                BrewStep(4, "Serví", "Diluí más o menos 1:1 con agua o leche y bastante hielo.", manualHint = "Diluir 1:1", why = "Es un concentrado fuerte: tomarlo sin diluir sería demasiado intenso."),
            ),
            flavorNotes = listOf("Suave", "Dulce", "Baja acidez"),
            tips = listOf(
                "Reposos más largos dan más fuerza, pero pasadas las 18 h se pone amaderado.",
                "Es un concentrado — siempre diluilo antes de tomar.",
            ),
            beginnerIntro = "El cold brew es café hecho con agua fría y mucha paciencia: dejás el café en remojo de un día para el otro en la heladera. Como nunca se calienta, sale dulce, suave y muy poco ácido. Lo que obtenés es un concentrado fuerte que después diluís con agua o leche y hielo. Cero técnica: solo medir, esperar y colar.",
            equipment = listOf(
                "Un frasco o jarra grande con tapa",
                "Filtro de papel o una tela para colar",
                "80 g de café molido extra grueso",
                "1 litro (1000 g) de agua fría",
                "Lugar en la heladera",
            ),
            glossary = listOf(
                GlossaryTerm("Concentrado", "Un café fuerte pensado para diluir con agua o leche antes de tomarlo, no para tomar solo."),
                GlossaryTerm("Baja acidez", "Sabor poco \"ácido\" o cítrico. El agua fría extrae menos acidez, por eso el cold brew es tan suave."),
                GlossaryTerm("Poso", "El café molido que queda en el fondo una vez listo. Hay que colarlo bien."),
                grindTerm,
            ),
        )
    }
}
