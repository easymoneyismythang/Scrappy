package com.scrappy.app.data

object ScrappyRepository {
    val metals = listOf(
        Metal("Copper #1","Copper",3.65,false,"bright reddish copper","Clean, uncoated copper wire/tubing with yard-specific rules."),
        Metal("Copper #2","Copper",3.35,false,"darker/oxidized copper","Copper with solder, paint, oxidation, or other allowed contamination."),
        Metal("Bare Bright Copper","Copper",3.90,false,"clean bright copper","Often among the highest-value common scrap grades; verify strand/size rules."),
        Metal("Brass","Copper alloy",2.15,false,"yellow-gold","Common in valves, fittings and plumbing hardware."),
        Metal("Aluminum","Non-ferrous",0.72,false,"silver-gray","Lightweight; grade varies widely by extrusion, cast, sheet and contamination."),
        Metal("Stainless Steel","Steel alloy",0.42,false,"silver-gray","Some grades are weakly magnetic; nickel content can affect value."),
        Metal("Steel","Ferrous",0.09,true,"gray/black","Usually magnetic and lower value per pound; weight can still matter."),
        Metal("Cast Iron","Ferrous",0.11,true,"dark gray","Heavy and brittle; often accepted as prepared or unprepared iron."),
        Metal("Lead","Non-ferrous",0.55,false,"dull gray","Dense and soft. Avoid dust and wash hands after handling."),
        Metal("Zinc","Non-ferrous",0.60,false,"bluish silver","Often found in die-cast items and galvanized coatings.")
    )

    val yards = listOf(
        Yard("demo-near","DEMO • Nearby Yard A",4.2,"","","Fictional demo only", mapOf("Copper #1" to 3.65,"Copper #2" to 3.35,"Bare Bright Copper" to 3.90,"Aluminum" to .72,"Brass" to 2.15,"Steel" to .09,"Stainless Steel" to .42,"Cast Iron" to .11,"Lead" to .55,"Zinc" to .60)),
        Yard("demo-mid","DEMO • Nearby Yard B",7.8,"","","Fictional demo only", mapOf("Copper #1" to 3.78,"Copper #2" to 3.42,"Bare Bright Copper" to 3.95,"Aluminum" to .68,"Brass" to 2.08,"Steel" to .10,"Stainless Steel" to .44,"Cast Iron" to .12,"Lead" to .58,"Zinc" to .57)),
        Yard("demo-far","DEMO • Nearby Yard C",11.1,"","","Fictional demo only", mapOf("Copper #1" to 3.55,"Copper #2" to 3.30,"Bare Bright Copper" to 3.82,"Aluminum" to .76,"Brass" to 2.20,"Steel" to .08,"Stainless Steel" to .40,"Cast Iron" to .10,"Lead" to .53,"Zinc" to .62))
    )

    fun priceFor(metal: String): Double = metals.firstOrNull { it.name == metal }?.benchmarkPerLb ?: 0.0
}
