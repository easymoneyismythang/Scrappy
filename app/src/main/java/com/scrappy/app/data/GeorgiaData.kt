package com.scrappy.app.data

import java.util.Calendar

/**
 * Real Georgia scrap figures and real Covington, GA yards.
 *
 * Prices come from the Covington take-in sheet state (Nov 26, 2026) and are
 * index guidance only — never a guaranteed payout. No price or percentage
 * change is shown unless it was actually published; unknown deltas are omitted.
 * Never add yards here without verifying them first.
 */
data class GeorgiaPrice(
    val grade: String,
    val category: String,
    val pricePerLb: Double,
    val source: String,
    val priceDate: String
)

object GeorgiaPrices {
    const val updatedLabel = "Updated Nov 26, 2026"

    val rows = listOf(
        GeorgiaPrice("Bare Bright Copper", "Copper", 4.13, "priceofscrapmetals.com Georgia page", "Nov 26, 2026"),
        GeorgiaPrice("Copper #1", "Copper", 4.58, "scrapmetalindex.com Georgia page (3 reporting yards)", "Nov 25, 2026"),
        GeorgiaPrice("Copper #2", "Copper", 4.40, "scrapmetalindex.com Georgia page (3 reporting yards)", "Nov 25, 2026"),
        GeorgiaPrice("#1 Insulated Wire", "Copper", 2.70, "scrapmetalindex.com Georgia page (3 reporting yards)", "Nov 25, 2026")
    )

    const val comexNote =
        "COMEX copper \$5.46/lb, up \$0.13 (+2.34%) today — SELL signal. " +
        "The Georgia index is down 7.8% on the week, so signals are mixed."

    const val sourceLine =
        "Georgia figures: priceofscrapmetals.com (Nov 26, 2026) and scrapmetalindex.com " +
        "Georgia page (Nov 25, 2026, 3 reporting yards). Index guidance — confirm with the yard."

    /** Filter-chip category for a repository metal name. */
    fun categoryOf(metalName: String): String = when (metalName) {
        "Copper #1", "Copper #2", "Bare Bright Copper" -> "Copper"
        "Brass" -> "Brass"
        "Aluminum" -> "Aluminum"
        "Steel", "Cast Iron" -> "Steel"
        "Stainless Steel" -> "Stainless"
        else -> "Other"
    }

    /**
     * Georgia index price for an AI metal group. The AI cannot tell copper
     * grades apart, so copper uses the conservative Copper #2 index figure.
     * Null means no Georgia index price exists for that group.
     */
    fun priceForGroup(group: String): GeorgiaPrice? = when (group) {
        "Copper" -> rows.firstOrNull { it.grade == "Copper #2" }
        else -> null
    }

    fun sortingTip(group: String): String = when (group) {
        "Copper" -> "Remove fittings, solder and non-copper materials for the best price. " +
            "Strip insulation only if the yard pays more for bare wire than the extra work costs."
        "Brass" -> "Separate brass from copper — mixed loads pay the lower grade. " +
            "Remove steel screws and plastic handles."
        "Aluminum" -> "Separate cast, extrusion and sheet — grades pay differently. " +
            "Remove steel screws, rubber and plastic."
        "Steel/Iron" -> "Check with a magnet and separate heavy steel from light iron. " +
            "Remove wood, rubber and non-metal parts."
        "Stainless Steel" -> "Some grades are weakly magnetic. Ask the yard which grades they buy — " +
            "nickel content changes the price."
        "Lead" -> "Dense and soft. Avoid dust, wash hands after handling, " +
            "and keep lead separate from other metals."
        "Zinc" -> "Often die-cast or galvanized coatings. Keep zinc separate — " +
            "mixed loads pay less."
        else -> "Use the identification helper to narrow it down, and confirm the grade " +
            "with the yard before selling."
    }
}

data class RealYard(
    val name: String,
    val address: String,
    val phone: String,
    val hours: String,
    /** True when Saturday hours were never published for this yard. */
    val saturdayUnknown: Boolean = false
)

object RealYards {
    val yards = listOf(
        RealYard(
            "LB Recycling Center",
            "8194 Washington St SW, Covington, GA",
            "770-786-5119",
            "Mon–Fri 8:00 AM–5:00 PM, Sat 8:00 AM–3:00 PM, Sun closed"
        ),
        RealYard(
            "Allied Recycling Inc",
            "11164 GA-36, Covington, GA",
            "770-385-1881",
            "Mon–Fri 8:00 AM–5:00 PM, Sat 8:00 AM–3:00 PM, Sun closed"
        ),
        RealYard(
            "Oconee Metal Recovery",
            "9193 Washington St SW, Covington, GA",
            "770-385-0087",
            "Mon–Fri 8:00 AM–5:15 PM, Sat hours not published — call first",
            saturdayUnknown = true
        )
    )

    /** Closest yard, used for the home-screen card and the result "Get directions" button. */
    val closest: RealYard get() = yards[0]

    /**
     * Open/closed status from the yard's regular hours and the device clock.
     * Holiday closures are not in these hours — the screens remind the user
     * to call before driving on holidays.
     */
    fun statusAt(yard: RealYard, now: Calendar = Calendar.getInstance()): String {
        val day = now.get(Calendar.DAY_OF_WEEK)
        val mins = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val isWeekday = day in Calendar.MONDAY..Calendar.FRIDAY
        val isSaturday = day == Calendar.SATURDAY
        val closeMins = if (yard.saturdayUnknown) 17 * 60 + 15 else 17 * 60
        val closeLabel = if (yard.saturdayUnknown) "5:15 PM" else "5:00 PM"
        return when {
            day == Calendar.SUNDAY ->
                if (yard.saturdayUnknown) "Closed today (Sunday hours unverified)" else "Closed today"
            isSaturday && yard.saturdayUnknown -> "Call first — Saturday hours not published"
            isSaturday -> when {
                mins in 480 until 900 -> "Open now · Closes 3:00 PM"
                mins < 480 -> "Closed · Opens 8:00 AM today"
                else -> "Closed · Opens 8:00 AM Mon"
            }
            isWeekday -> when {
                mins in 480 until closeMins -> "Open now · Closes $closeLabel"
                mins < 480 -> "Closed · Opens 8:00 AM today"
                day == Calendar.FRIDAY -> "Closed · Opens 8:00 AM Sat"
                else -> "Closed · Opens 8:00 AM tomorrow"
            }
            else -> "Call for hours"
        }
    }
}
