package com.scrappy.app.data

import java.time.LocalDate

object DailyVerse {
    data class Verse(val reference: String, val words: String)
    val verses = listOf(
        Verse("Philippians 4:13", "I can do all things through Christ which strengtheneth me."),
        Verse("Psalm 23:1", "The LORD is my shepherd; I shall not want."),
        Verse("Psalm 46:1", "God is our refuge and strength, a very present help in trouble."),
        Verse("Proverbs 3:5", "Trust in the LORD with all thine heart; and lean not unto thine own understanding."),
        Verse("Proverbs 3:6", "In all thy ways acknowledge him, and he shall direct thy paths."),
        Verse("Psalm 56:3", "What time I am afraid, I will trust in thee."),
        Verse("Psalm 118:24", "This is the day which the LORD hath made; we will rejoice and be glad in it."),
        Verse("Psalm 119:105", "Thy word is a lamp unto my feet, and a light unto my path."),
        Verse("1 Thessalonians 5:16", "Rejoice evermore."),
        Verse("1 Thessalonians 5:17", "Pray without ceasing."),
        Verse("1 Thessalonians 5:18", "In every thing give thanks: for this is the will of God in Christ Jesus concerning you."),
        Verse("1 Corinthians 16:14", "Let all your things be done with charity."),
        Verse("Romans 12:12", "Rejoicing in hope; patient in tribulation; continuing instant in prayer;"),
        Verse("Psalm 37:5", "Commit thy way unto the LORD; trust also in him; and he shall bring it to pass."),
        Verse("Proverbs 16:3", "Commit thy works unto the LORD, and thy thoughts shall be established."),
        Verse("Psalm 27:1", "The LORD is my light and my salvation; whom shall I fear? the LORD is the strength of my life; of whom shall I be afraid?"),
        Verse("Psalm 121:2", "My help cometh from the LORD, which made heaven and earth."),
        Verse("Psalm 150:6", "Let every thing that hath breath praise the LORD. Praise ye the LORD."),
        Verse("Matthew 5:9", "Blessed are the peacemakers: for they shall be called the children of God."),
        Verse("Matthew 5:8", "Blessed are the pure in heart: for they shall see God."),
        Verse("Matthew 5:7", "Blessed are the merciful: for they shall obtain mercy."),
        Verse("Matthew 11:28", "Come unto me, all ye that labour and are heavy laden, and I will give you rest."),
        Verse("1 Peter 5:7", "Casting all your care upon him; for he careth for you."),
        Verse("Psalm 34:8", "O taste and see that the LORD is good: blessed is the man that trusteth in him."),
        Verse("Psalm 31:24", "Be of good courage, and he shall strengthen your heart, all ye that hope in the LORD."),
        Verse("Psalm 37:4", "Delight thyself also in the LORD; and he shall give thee the desires of thine heart."),
        Verse("Proverbs 17:22", "A merry heart doeth good like a medicine: but a broken spirit drieth the bones."),
        Verse("Psalm 100:2", "Serve the LORD with gladness: come before his presence with singing."),
        Verse("Psalm 100:4", "Enter into his gates with thanksgiving, and into his courts with praise: be thankful unto him, and bless his name."),
        Verse("Psalm 100:5", "For the LORD is good; his mercy is everlasting; and his truth endureth to all generations."),
        Verse("Psalm 136:1", "O give thanks unto the LORD; for he is good: for his mercy endureth for ever.")
    )
    // Local calendar dates remain stable across restarts and daylight saving changes.
    // The offline collection repeats after 31 days, never on consecutive days.
    fun forDate(date: LocalDate = LocalDate.now()): Verse =
        verses[Math.floorMod(date.toEpochDay(), verses.size.toLong()).toInt()]
}
