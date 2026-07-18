import { Injectable, signal } from '@angular/core';
import { AshtakootResult, KootaScore } from './models';

export type Lang = 'en' | 'hi';

const STORAGE_KEY = 'kundli.lang';

/** UI strings. Every key exists in both languages. */
const MESSAGES: Record<Lang, Record<string, string>> = {
  en: {
    'invocation': '॥ श्री गणेशाय नमः ॥',
    'brand.sub': 'Vedic Compatibility',
    'nav.charts': 'Charts',
    'nav.match': 'Match',
    'health.up': 'Ephemeris: full precision',
    'health.down': 'Ephemeris: unavailable',
    'footer.note': 'Lahiri ayanamsa · Swiss Ephemeris',

    // ---- Date field (dd · month · yyyy) ----
    'date.day': 'Day',
    'date.month': 'Month',
    'date.year': 'Year',
    'date.dayPh': 'DD',
    'date.monthPh': 'Month',
    'date.yearPh': 'YYYY',

    // ---- Place picker (online lookup) ----
    'place.searching': 'Searching more places…',
    'place.online': 'Online result',

    'charts.title': 'Birth Charts',
    'charts.intro':
      'Enter the exact birth time — to the minute. The Moon moves ~13° a day, so near a Nakshatra boundary even a small error can change the match.',
    'charts.form.title': 'New chart',
    'charts.name': 'Name',
    'charts.name.ph': 'Person’s name',
    'charts.date': 'Birth date',
    'charts.time': 'Birth time',
    'charts.time.hint': 'As on the birth certificate — mind AM/PM',
    'charts.place': 'Place of birth',
    'charts.place.ph': 'Type city name… e.g. Patna or पटना',
    'charts.place.noResults': 'No matching city — enter location manually below',
    'charts.manual.show': 'City not in the list? Enter location manually',
    'charts.manual.hide': 'Hide manual location entry',
    'charts.manual.place': 'Place name',
    'charts.manual.place.ph': 'Village / town, District',
    'charts.tz': 'Timezone of birthplace',
    'charts.lat': 'Latitude',
    'charts.lon': 'Longitude',
    'charts.submit': 'Compute chart ✦',
    'charts.submitting': 'Computing chart…',
    'charts.empty': 'No charts yet. Create the first one to begin matching.',
    'charts.moonSign': 'Moon sign',
    'charts.moonNak': 'Moon nakshatra & pada',
    'charts.pada': 'Pada',
    'charts.attr.title': 'Kundli details',
    'charts.attr.gana': 'Gana',
    'charts.attr.nadi': 'Nadi',
    'charts.attr.yoni': 'Yoni',
    'charts.attr.varna': 'Varna',
    'charts.attr.vashya': 'Vashya',
    'charts.attr.moonLord': 'Rashi lord',
    'charts.house7.title': '7th house (marriage)',
    'charts.house7.sign': 'Sign',
    'charts.house7.lord': 'Lord',
    'charts.house7.occupants': 'Planets in 7th',
    'charts.house7.empty': 'No planets — clear',
    'charts.house7.FAVOURABLE': 'Favourable',
    'charts.house7.MIXED': 'Mixed',
    'charts.house7.NEEDS_ATTENTION': 'Needs attention',
    'charts.house7.note': 'A first-pass indicator from the planets sitting in the 7th house counted from the Lagna. It does not weigh aspects, the 7th lord\'s strength or dashas — an astrologer should review before any conclusion.',
    'charts.delete': 'Delete',
    'charts.deleteAria': 'Delete chart for',
    'charts.deleteConfirm': 'Delete the chart for “{name}”?',
    'charts.cta': 'Run a match →',
    'charts.err.load': 'Could not load charts — is the backend running?',
    'charts.err.compute': 'Chart computation failed.',
    'charts.err.delete': 'Delete failed.',

    'match.title': 'Match',
    'match.intro': 'Ashtakoot — the eight-fold compatibility of two Moon charts, out of 36 points.',
    'match.needTwo': 'You need at least two charts to run a match.',
    'match.createCharts': 'Create charts →',
    'match.boy': 'Boy’s chart',
    'match.girl': 'Girl’s chart',
    'match.select': 'Select…',
    'match.swap': 'Swap boy and girl charts',
    'match.run': 'Match ✦',
    'match.running': 'Matching…',
    'match.err.same': 'Pick two different charts.',
    'match.err.failed': 'Match failed.',
    'match.err.load': 'Could not load charts.',
    'match.kootas': 'Koota breakdown',
    'match.doshas': 'Doshas',
    'match.manglik': 'Manglik (Mangal Dosha)',
    'match.rules': 'Rule set',

    'score.of': 'of {max}',
    'score.aria': 'Score {points} of {max} points',

    'verdict.notRecommended': 'Not recommended (below 18)',
    'verdict.acceptable': 'Acceptable',
    'verdict.good': 'Good',
    'verdict.excellent': 'Excellent',
    'verdict.noDosha': 'no effective dosha',
    'verdict.doshaApplies': 'but {doshas} dosha applies (not cancelled); review carefully',

    'dosha.none': 'No {name} dosha',
    'dosha.cancelled': '{name} dosha cancelled',
    'dosha.applies': '{name} dosha applies',

    // ---- Manglik (Kuja / Mangal Dosha) ----
    'manglik.personA': '{name}',
    'manglik.personB': '{name}',
    'manglik.state.NOT_MANGLIK': 'Not Manglik',
    'manglik.state.MANGLIK': 'Manglik',
    'manglik.state.PARTIAL_MANGLIK': 'Partial Manglik',
    'manglik.ref.LAGNA': 'Ascendant',
    'manglik.ref.MOON': 'Moon',
    'manglik.ref.VENUS': 'Venus',
    'manglik.house.n': 'Mars in house {n}',
    'manglik.house.trigger': 'Mars in house {n} — Manglik',
    'manglik.triggered.none': 'No Manglik from any reference',
    'manglik.triggered.list': 'Manglik from {refs}',
    'manglik.compat.NEITHER_MANGLIK': 'Neither partner is Manglik',
    'manglik.compat.REQUIRES_DETAILED_REVIEW': 'Detailed review required',
    'manglik.rules': 'Manglik rule set',
    'manglik.note': 'Mars in houses 1, 2, 4, 7, 8, 12 from Lagna, Moon or Venus flags Manglik. Mars in own sign (Aries/Scorpio), exalted (Capricorn) or debilitated (Cancer) cancels the dosha (classical Parashari rule). Other advanced cancellations are NOT included — an astrologer should review any remaining Manglik presence.',
    'manglik.cancellation.MARS_IN_OWN_SIGN': 'Cancelled: Mars in own sign',
    'manglik.cancellation.MARS_EXALTED': 'Cancelled: Mars exalted',
    'manglik.cancellation.MARS_DEBILITATED': 'Cancelled: Mars debilitated',

    // ---- Mother-mode ----
    'mother.home.namaste': 'Namaste 🙏',
    'mother.home.readyLine': "{name}'s chart is ready",
    'mother.home.newMatch': 'Match a new chart',
    'mother.home.history': 'Previous matches',
    'mother.home.advanced': 'Advanced (charts, settings)',
    'mother.setup.title': 'Set up your primary profile',
    'mother.setup.body': 'Create a birth chart first, then mark it as your primary profile. Every future match runs against this profile.',
    'mother.setup.cta': 'Set up →',
    'advanced.primary.set': 'Set as primary',
    'advanced.primary.already': 'Primary ✓',

    'newMatch.title': 'New match',
    'newMatch.intro': 'Enter only the prospective bride\'s details. The primary profile is used as the boy.',
    'newMatch.girlName': 'Bride\'s name',
    'newMatch.girlNamePh': 'Bride\'s name',
    'newMatch.date': 'Birth date',
    'newMatch.time': 'Birth time',
    'newMatch.timeHint': 'Exact time to the minute; mind AM/PM',
    'newMatch.place': 'Birthplace',
    'newMatch.next': 'Next',
    'newMatch.needProfile': 'Set up the primary profile first.',
    'newMatch.err.compute': 'Chart calculation failed.',
    'newMatch.err.match': 'Matching failed.',

    'confirm.title': 'Please confirm',
    'confirm.name': 'Name',
    'confirm.date': 'Birth date',
    'confirm.time': 'Birth time',
    'confirm.place': 'Birthplace',
    'confirm.question': 'Is this information correct?',
    'confirm.yes': 'Yes, match',
    'confirm.edit': 'Change details',
    'confirm.matching': 'Matching…',

    'motherResult.total': 'Total gunas',
    'motherResult.category.STRONG': 'Good preliminary match',
    'motherResult.category.MODERATE': 'Moderate match — detailed review needed',
    'motherResult.category.WEAK': 'Weak preliminary match',
    'motherResult.nadi': 'Nadi Dosha',
    'motherResult.bhakoot': 'Bhakoot Dosha',
    'motherResult.manglik': 'Manglik status',
    'motherResult.dosha.yes': 'Yes',
    'motherResult.dosha.no': 'No',
    'motherResult.dosha.cancelled': 'Cancelled',
    'motherResult.manglik.NEITHER_MANGLIK': 'Not Manglik',
    'motherResult.manglik.REQUIRES_DETAILED_REVIEW': 'Detailed review required',
    'motherResult.manglik.viewDetail': 'See the full report',
    'motherResult.breakdown.show': 'View full breakdown ▾',
    'motherResult.breakdown.hide': 'Hide full breakdown ▴',
    'motherResult.breakdown.kootas': 'Eight-koota breakdown',
    'motherResult.breakdown.doshas': 'Doshas',
    'motherResult.breakdown.manglik': 'Manglik (Mangal Dosha)',
    'motherResult.disclaimer': 'This is a preliminary Kundli check only. Do not decide on marriage based on this report alone. If the preliminary result is good, please consult an experienced astrologer for a detailed review.',
    'motherResult.newMatchAgain': 'Match another chart',
    'motherResult.backHome': 'Home',
    'motherResult.pdf': '📄 PDF',
    'motherResult.share': '↗ Share',

    'history.title': 'Previous matches',
    'history.empty': 'No matches yet.',
    'history.newMatch': 'New match',
    'history.match': 'Match',

    // ---- PDF / Print report ----
    'report.title': 'Kundli Matching Report',
    'report.subtitle': 'Preliminary compatibility check',
    'report.section.profiles': 'Birth profiles',
    'report.section.result': 'Preliminary result',
    'report.section.kootas': 'Eight-koota breakdown',
    'report.section.doshas': 'Nadi & Bhakoot',
    'report.section.manglik': 'Manglik (Mangal Dosha)',
    'report.section.warnings': 'Accuracy notes',
    'report.section.rules': 'Calculation metadata',
    'report.person.name': 'Name',
    'report.person.date': 'Birth date',
    'report.person.time': 'Birth time',
    'report.person.place': 'Birthplace',
    'report.person.timezone': 'Timezone',
    'report.person.moonRashi': 'Moon Rashi',
    'report.person.moonNakshatra': 'Moon Nakshatra',
    'report.person.pada': 'Pada',
    'report.ashutosh.timeConfirm': 'Confirmed 16:20 (4:20 PM) — not 4:20 AM.',
    'report.total': 'Total gunas',
    'report.category': 'Preliminary result',
    'report.rules.ayanamsa': 'Ayanamsa',
    'report.rules.matching': 'Matching system',
    'report.rules.ashtakoota': 'Ashtakoot rules',
    'report.rules.manglik': 'Manglik rules',
    'report.rules.ephemeris': 'Ephemeris mode',
    'report.rules.reportId': 'Report ID',
    'report.rules.printedAt': 'Printed at',
    'report.koota.header.koota': 'Koota',
    'report.koota.header.boy': 'Boy',
    'report.koota.header.girl': 'Girl',
    'report.koota.header.score': 'Score',
    'report.manglik.person': 'Person',
    'report.manglik.state': 'Status',
    'report.manglik.fromLagna': 'From Lagna',
    'report.manglik.fromMoon': 'From Moon',
    'report.manglik.fromVenus': 'From Venus',
    'report.manglik.house': 'H {n}',
    'report.disclaimer.title': 'Important',
    'report.print': 'Print / Save as PDF',
    'report.close': 'Close',
    'report.strengths': 'Positive points',
    'report.attention': 'Points needing attention',
    'report.strength.nadiOk': 'No Nadi Dosha',
    'report.strength.bhakootOk': 'No Bhakoot Dosha',
    'report.strength.manglikOk': 'Neither partner is Manglik',
    'report.strength.varnaOk': 'Varna full',
    'report.strength.taraFull': 'Tara full',
    'report.strength.categoryStrong': 'Strong preliminary total',
    'report.attention.nadi': 'Nadi Dosha present',
    'report.attention.bhakoot': 'Bhakoot Dosha present',
    'report.attention.manglik': 'Manglik presence — detailed review needed',
    'report.attention.categoryWeak': 'Total below moderate threshold',
    'report.attention.boundary': 'Moon near a Rashi or Nakshatra boundary — the result may change with a small birth-time correction',
    'report.attention.historicalTz': 'Birth predates 1950 — verify historical timezone offset',
    'noneListed': 'None listed.',
  },
  hi: {
    'invocation': '॥ श्री गणेशाय नमः ॥',
    'brand.sub': 'वैदिक कुंडली मिलान',
    'nav.charts': 'कुंडली',
    'nav.match': 'मिलान',
    'health.up': 'पंचांग गणना: पूर्ण सटीक',
    'health.down': 'पंचांग गणना: अनुपलब्ध',
    'footer.note': 'लहिरी अयनांश · स्विस एफ़ेमेरिस',

    // ---- तिथि फ़ील्ड (dd · माह · yyyy) ----
    'date.day': 'दिन',
    'date.month': 'माह',
    'date.year': 'वर्ष',
    'date.dayPh': 'दिन',
    'date.monthPh': 'माह',
    'date.yearPh': 'वर्ष',

    // ---- स्थान खोज (ऑनलाइन) ----
    'place.searching': 'और स्थान खोजे जा रहे हैं…',
    'place.online': 'ऑनलाइन परिणाम',

    'charts.title': 'जन्म कुंडली',
    'charts.intro':
      'जन्म का सही समय भरें — मिनट तक। चंद्रमा एक दिन में लगभग 13° चलता है, इसलिए नक्षत्र की सीमा के पास थोड़ी सी चूक भी मिलान बदल सकती है।',
    'charts.form.title': 'नई कुंडली',
    'charts.name': 'नाम',
    'charts.name.ph': 'व्यक्ति का नाम',
    'charts.date': 'जन्म तिथि',
    'charts.time': 'जन्म समय',
    'charts.time.hint': 'जन्म प्रमाणपत्र के अनुसार — AM/PM का ध्यान रखें',
    'charts.place': 'जन्म स्थान',
    'charts.place.ph': 'शहर का नाम लिखें… जैसे पटना या Patna',
    'charts.place.noResults': 'कोई शहर नहीं मिला — नीचे स्थान स्वयं भरें',
    'charts.manual.show': 'शहर सूची में नहीं है? स्थान स्वयं भरें',
    'charts.manual.hide': 'मैनुअल स्थान प्रविष्टि छिपाएँ',
    'charts.manual.place': 'स्थान का नाम',
    'charts.manual.place.ph': 'गाँव / कस्बा, ज़िला',
    'charts.tz': 'जन्म स्थान का समय क्षेत्र',
    'charts.lat': 'अक्षांश (Latitude)',
    'charts.lon': 'देशांतर (Longitude)',
    'charts.submit': 'कुंडली बनाएँ ✦',
    'charts.submitting': 'कुंडली बन रही है…',
    'charts.empty': 'अभी कोई कुंडली नहीं है। मिलान शुरू करने के लिए पहली कुंडली बनाएँ।',
    'charts.moonSign': 'चंद्र राशि',
    'charts.moonNak': 'चंद्र नक्षत्र और चरण',
    'charts.pada': 'चरण',
    'charts.attr.title': 'कुंडली विवरण',
    'charts.attr.gana': 'गण',
    'charts.attr.nadi': 'नाड़ी',
    'charts.attr.yoni': 'योनि',
    'charts.attr.varna': 'वर्ण',
    'charts.attr.vashya': 'वश्य',
    'charts.attr.moonLord': 'राशि स्वामी',
    'charts.house7.title': 'सप्तम भाव (विवाह)',
    'charts.house7.sign': 'राशि',
    'charts.house7.lord': 'स्वामी',
    'charts.house7.occupants': 'सप्तम में ग्रह',
    'charts.house7.empty': 'कोई ग्रह नहीं — निर्मल',
    'charts.house7.FAVOURABLE': 'अनुकूल',
    'charts.house7.MIXED': 'मिश्रित',
    'charts.house7.NEEDS_ATTENTION': 'ध्यान देने योग्य',
    'charts.house7.note': 'यह लग्न से गिने गए सप्तम भाव में बैठे ग्रहों पर आधारित प्रारंभिक संकेत मात्र है। इसमें दृष्टि, सप्तमेश का बल या दशा शामिल नहीं है — किसी निष्कर्ष से पहले ज्योतिषी से परामर्श करें।',
    'charts.delete': 'हटाएँ',
    'charts.deleteAria': 'कुंडली हटाएँ:',
    'charts.deleteConfirm': '“{name}” की कुंडली हटाएँ?',
    'charts.cta': 'मिलान करें →',
    'charts.err.load': 'कुंडली लोड नहीं हो सकीं — क्या सर्वर चल रहा है?',
    'charts.err.compute': 'कुंडली की गणना विफल रही।',
    'charts.err.delete': 'हटाने में विफल।',

    'match.title': 'गुण मिलान',
    'match.intro': 'अष्टकूट — दो चंद्र कुंडलियों का आठ कूटों पर मिलान, कुल 36 गुणों में से।',
    'match.needTwo': 'मिलान के लिए कम से कम दो कुंडलियाँ चाहिए।',
    'match.createCharts': 'कुंडली बनाएँ →',
    'match.boy': 'वर (लड़के) की कुंडली',
    'match.girl': 'वधू (लड़की) की कुंडली',
    'match.select': 'चुनें…',
    'match.swap': 'वर और वधू की कुंडली अदला-बदली करें',
    'match.run': 'मिलान करें ✦',
    'match.running': 'मिलान हो रहा है…',
    'match.err.same': 'दो अलग-अलग कुंडलियाँ चुनें।',
    'match.err.failed': 'मिलान विफल रहा।',
    'match.err.load': 'कुंडली लोड नहीं हो सकीं।',
    'match.kootas': 'कूट विवरण',
    'match.doshas': 'दोष',
    'match.manglik': 'मंगल दोष',
    'match.rules': 'नियम संस्करण',

    'score.of': '{max} में से',
    'score.aria': '{max} में से {points} गुण',

    'verdict.notRecommended': 'अनुशंसित नहीं (18 से कम)',
    'verdict.acceptable': 'स्वीकार्य',
    'verdict.good': 'अच्छा',
    'verdict.excellent': 'उत्तम',
    'verdict.noDosha': 'कोई प्रभावी दोष नहीं',
    'verdict.doshaApplies': 'लेकिन {doshas} दोष लागू है (निरस्त नहीं); सावधानी से विचार करें',

    'dosha.none': '{name} दोष नहीं है',
    'dosha.cancelled': '{name} दोष निरस्त (भंग)',
    'dosha.applies': '{name} दोष लागू है',

    // ---- मंगल दोष ----
    'manglik.personA': '{name}',
    'manglik.personB': '{name}',
    'manglik.state.NOT_MANGLIK': 'मंगल दोष नहीं',
    'manglik.state.MANGLIK': 'मंगल दोष है',
    'manglik.state.PARTIAL_MANGLIK': 'आंशिक मंगल दोष',
    'manglik.ref.LAGNA': 'लग्न',
    'manglik.ref.MOON': 'चंद्र',
    'manglik.ref.VENUS': 'शुक्र',
    'manglik.house.n': 'मंगल {n} भाव में',
    'manglik.house.trigger': 'मंगल {n} भाव में — दोष',
    'manglik.triggered.none': 'किसी भी संदर्भ से मंगल दोष नहीं',
    'manglik.triggered.list': '{refs} से मंगल दोष',
    'manglik.compat.NEITHER_MANGLIK': 'दोनों में मंगल दोष नहीं है',
    'manglik.compat.REQUIRES_DETAILED_REVIEW': 'विस्तृत जांच आवश्यक',
    'manglik.rules': 'मंगल दोष नियम',
    'manglik.note': 'लग्न, चंद्र या शुक्र से मंगल का 1, 2, 4, 7, 8, 12 भाव में होना मंगल दोष माना जाता है। मंगल स्वराशि (मेष/वृश्चिक), उच्च (मकर) या नीच (कर्क) में हो तो दोष स्वयं निरस्त हो जाता है (पराशरी नियम)। अन्य जटिल भंग नियम शामिल नहीं हैं — शेष मंगल दोष की स्थिति में किसी अनुभवी ज्योतिषी से विस्तृत जांच कराएँ।',
    'manglik.cancellation.MARS_IN_OWN_SIGN': 'निरस्त: मंगल स्वराशि में',
    'manglik.cancellation.MARS_EXALTED': 'निरस्त: मंगल उच्च का',
    'manglik.cancellation.MARS_DEBILITATED': 'निरस्त: मंगल नीच का',

    // ---- मदर मोड ----
    'mother.home.namaste': 'नमस्ते माँ 🙏',
    'mother.home.readyLine': '{name} की कुंडली तैयार है',
    'mother.home.newMatch': 'नई कुंडली मिलाएँ',
    'mother.home.history': 'पिछले मिलान देखें',
    'mother.home.advanced': 'उन्नत विकल्प (कुंडली, सेटिंग्स)',
    'mother.setup.title': 'पहले प्राथमिक कुंडली सेट करें',
    'mother.setup.body': 'पहले एक कुंडली बनाएँ और उसे प्राथमिक कुंडली के रूप में चुनें। हर नया मिलान इसी कुंडली से होगा।',
    'mother.setup.cta': 'सेट करें →',
    'advanced.primary.set': 'प्राथमिक बनाएँ',
    'advanced.primary.already': 'प्राथमिक ✓',

    'newMatch.title': 'नया मिलान',
    'newMatch.intro': 'केवल वधू (लड़की) की जानकारी भरें। वर (लड़के) की कुंडली अपने-आप चुन ली जाएगी।',
    'newMatch.girlName': 'वधू का नाम',
    'newMatch.girlNamePh': 'वधू का नाम',
    'newMatch.date': 'जन्म तिथि',
    'newMatch.time': 'जन्म समय',
    'newMatch.timeHint': 'मिनट तक सही समय; AM/PM का ध्यान रखें',
    'newMatch.place': 'जन्म स्थान',
    'newMatch.next': 'आगे बढ़ें',
    'newMatch.needProfile': 'पहले प्राथमिक कुंडली सेट करें।',
    'newMatch.err.compute': 'कुंडली की गणना विफल रही।',
    'newMatch.err.match': 'मिलान विफल रहा।',

    'confirm.title': 'कृपया पुष्टि करें',
    'confirm.name': 'नाम',
    'confirm.date': 'जन्म तिथि',
    'confirm.time': 'जन्म समय',
    'confirm.place': 'जन्म स्थान',
    'confirm.question': 'क्या यह जानकारी सही है?',
    'confirm.yes': 'हाँ, मिलान करें',
    'confirm.edit': 'जानकारी बदलें',
    'confirm.matching': 'मिलान हो रहा है…',

    'motherResult.total': 'कुल गुण',
    'motherResult.category.STRONG': 'अच्छा प्रारंभिक मिलान',
    'motherResult.category.MODERATE': 'मध्यम मिलान — विस्तृत जांच आवश्यक',
    'motherResult.category.WEAK': 'कमजोर प्रारंभिक मिलान',
    'motherResult.nadi': 'नाड़ी दोष',
    'motherResult.bhakoot': 'भकूट दोष',
    'motherResult.manglik': 'मंगल स्थिति',
    'motherResult.dosha.yes': 'हाँ',
    'motherResult.dosha.no': 'नहीं',
    'motherResult.dosha.cancelled': 'निरस्त (भंग)',
    'motherResult.manglik.NEITHER_MANGLIK': 'मंगल दोष नहीं',
    'motherResult.manglik.REQUIRES_DETAILED_REVIEW': 'विस्तृत जांच आवश्यक',
    'motherResult.manglik.viewDetail': 'पूरी रिपोर्ट देखें',
    'motherResult.breakdown.show': 'पूरा विवरण देखें ▾',
    'motherResult.breakdown.hide': 'विवरण छिपाएँ ▴',
    'motherResult.breakdown.kootas': 'आठों गुणों का विवरण',
    'motherResult.breakdown.doshas': 'दोष',
    'motherResult.breakdown.manglik': 'मंगल दोष',
    'motherResult.disclaimer': 'यह परिणाम प्रारंभिक कुंडली जांच के लिए है। विवाह का निर्णय केवल इस रिपोर्ट के आधार पर न लें। अच्छे प्रारंभिक परिणाम के बाद किसी अनुभवी ज्योतिषी से विस्तृत जांच कराना उचित है।',
    'motherResult.newMatchAgain': 'दूसरा मिलान करें',
    'motherResult.backHome': 'होम',
    'motherResult.pdf': '📄 PDF',
    'motherResult.share': '↗ साझा करें',

    'history.title': 'पिछले मिलान',
    'history.empty': 'अभी कोई मिलान नहीं है।',
    'history.newMatch': 'नया मिलान',
    'history.match': 'मिलान',

    // ---- PDF / प्रिंट रिपोर्ट ----
    'report.title': 'कुंडली मिलान परिणाम',
    'report.subtitle': 'प्रारंभिक कुंडली जांच',
    'report.section.profiles': 'जन्म विवरण',
    'report.section.result': 'प्रारंभिक परिणाम',
    'report.section.kootas': 'आठों गुणों का विवरण',
    'report.section.doshas': 'नाड़ी और भकूट',
    'report.section.manglik': 'मंगल दोष',
    'report.section.warnings': 'शुद्धता संबंधी सूचनाएँ',
    'report.section.rules': 'गणना विवरण',
    'report.person.name': 'नाम',
    'report.person.date': 'जन्म तिथि',
    'report.person.time': 'जन्म समय',
    'report.person.place': 'जन्म स्थान',
    'report.person.timezone': 'समय क्षेत्र',
    'report.person.moonRashi': 'चंद्र राशि',
    'report.person.moonNakshatra': 'चंद्र नक्षत्र',
    'report.person.pada': 'चरण',
    'report.ashutosh.timeConfirm': 'पुष्टि: जन्म समय 16:20 (शाम 4:20) है — सुबह 4:20 नहीं।',
    'report.total': 'कुल गुण',
    'report.category': 'प्रारंभिक परिणाम',
    'report.rules.ayanamsa': 'अयनांश',
    'report.rules.matching': 'मिलान प्रणाली',
    'report.rules.ashtakoota': 'अष्टकूट नियम',
    'report.rules.manglik': 'मंगल दोष नियम',
    'report.rules.ephemeris': 'पंचांग गणना',
    'report.rules.reportId': 'रिपोर्ट संख्या',
    'report.rules.printedAt': 'प्रिंट समय',
    'report.koota.header.koota': 'कूट',
    'report.koota.header.boy': 'वर',
    'report.koota.header.girl': 'वधू',
    'report.koota.header.score': 'गुण',
    'report.manglik.person': 'व्यक्ति',
    'report.manglik.state': 'स्थिति',
    'report.manglik.fromLagna': 'लग्न से',
    'report.manglik.fromMoon': 'चंद्र से',
    'report.manglik.fromVenus': 'शुक्र से',
    'report.manglik.house': '{n} भाव',
    'report.disclaimer.title': 'सूचना',
    'report.print': 'प्रिंट / PDF के रूप में सहेजें',
    'report.close': 'बंद करें',
    'report.strengths': 'अच्छे पक्ष',
    'report.attention': 'ध्यान देने योग्य बातें',
    'report.strength.nadiOk': 'नाड़ी दोष नहीं है',
    'report.strength.bhakootOk': 'भकूट दोष नहीं है',
    'report.strength.manglikOk': 'दोनों में मंगल दोष नहीं है',
    'report.strength.varnaOk': 'वर्ण पूर्ण',
    'report.strength.taraFull': 'तारा पूर्ण',
    'report.strength.categoryStrong': 'कुल गुण अच्छे स्तर पर',
    'report.attention.nadi': 'नाड़ी दोष है',
    'report.attention.bhakoot': 'भकूट दोष है',
    'report.attention.manglik': 'मंगल दोष — विस्तृत जांच आवश्यक',
    'report.attention.categoryWeak': 'कुल गुण मध्यम स्तर से कम',
    'report.attention.boundary': 'चंद्रमा राशि या नक्षत्र की सीमा के पास है — जन्म समय में थोड़ा अंतर हो तो परिणाम बदल सकता है',
    'report.attention.historicalTz': 'जन्म 1950 से पहले — पुराने समय क्षेत्र की पुष्टि आवश्यक',
    'noneListed': 'कोई नहीं।',
  },
};

/** Fixed Vedic vocabularies, keyed by the exact names the backend emits. */
const RASHI_HI: Record<string, string> = {
  Mesha: 'मेष', Vrishabha: 'वृषभ', Mithuna: 'मिथुन', Karka: 'कर्क',
  Simha: 'सिंह', Kanya: 'कन्या', Tula: 'तुला', Vrishchika: 'वृश्चिक',
  Dhanu: 'धनु', Makara: 'मकर', Kumbha: 'कुंभ', Meena: 'मीन',
};

const NAKSHATRA_HI: Record<string, string> = {
  'Ashwini': 'अश्विनी', 'Bharani': 'भरणी', 'Krittika': 'कृत्तिका', 'Rohini': 'रोहिणी',
  'Mrigashira': 'मृगशिरा', 'Ardra': 'आर्द्रा', 'Punarvasu': 'पुनर्वसु', 'Pushya': 'पुष्य',
  'Ashlesha': 'आश्लेषा', 'Magha': 'मघा', 'Purva Phalguni': 'पूर्वा फाल्गुनी',
  'Uttara Phalguni': 'उत्तरा फाल्गुनी', 'Hasta': 'हस्त', 'Chitra': 'चित्रा', 'Swati': 'स्वाति',
  'Vishakha': 'विशाखा', 'Anuradha': 'अनुराधा', 'Jyeshtha': 'ज्येष्ठा', 'Mula': 'मूल',
  'Purva Ashadha': 'पूर्वाषाढ़ा', 'Uttara Ashadha': 'उत्तराषाढ़ा', 'Shravana': 'श्रवण',
  'Dhanishta': 'धनिष्ठा', 'Shatabhisha': 'शतभिषा', 'Purva Bhadrapada': 'पूर्व भाद्रपद',
  'Uttara Bhadrapada': 'उत्तर भाद्रपद', 'Revati': 'रेवती',
};

const KOOTA_HI: Record<string, string> = {
  'Varna': 'वर्ण', 'Vashya': 'वश्य', 'Tara': 'तारा', 'Yoni': 'योनि',
  'Graha Maitri': 'ग्रह मैत्री', 'Gana': 'गण', 'Bhakoot': 'भकूट', 'Nadi': 'नाड़ी',
};

const GRAHA_HI: Record<string, string> = {
  SUN: 'सूर्य', MOON: 'चंद्र', MARS: 'मंगल', MERCURY: 'बुध',
  JUPITER: 'गुरु', VENUS: 'शुक्र', SATURN: 'शनि',
  RAHU: 'राहु', KETU: 'केतु',
  Sun: 'सूर्य', Moon: 'चंद्र', Mars: 'मंगल', Mercury: 'बुध',
  Jupiter: 'गुरु', Venus: 'शुक्र', Saturn: 'शनि', Rahu: 'राहु', Ketu: 'केतु',
};

/**
 * Kundli-attribute vocabularies, keyed by the backend's stable UPPER_CASE codes.
 * Each entry is [English, Hindi]; unknown codes fall back to the raw code.
 */
const GANA_V: Record<string, [string, string]> = {
  DEVA: ['Deva', 'देव'], MANUSHYA: ['Manushya', 'मनुष्य'], RAKSHASA: ['Rakshasa', 'राक्षस'],
};
const NADI_V: Record<string, [string, string]> = {
  AADI: ['Aadi', 'आदि'], MADHYA: ['Madhya', 'मध्य'], ANTYA: ['Antya', 'अंत्य'],
};
const VARNA_V: Record<string, [string, string]> = {
  BRAHMIN: ['Brahmin', 'ब्राह्मण'], KSHATRIYA: ['Kshatriya', 'क्षत्रिय'],
  VAISHYA: ['Vaishya', 'वैश्य'], SHUDRA: ['Shudra', 'शूद्र'],
};
const VASHYA_V: Record<string, [string, string]> = {
  CHATUSHPADA: ['Chatushpada (quadruped)', 'चतुष्पद'], MANAVA: ['Manava (human)', 'मानव'],
  JALACHARA: ['Jalachara (aquatic)', 'जलचर'], VANACHARA: ['Vanachara (wild)', 'वनचर'],
  KEETA: ['Keeta (insect)', 'कीट'],
};
const YONI_V: Record<string, [string, string]> = {
  HORSE: ['Horse', 'अश्व'], ELEPHANT: ['Elephant', 'गज'], SHEEP: ['Sheep', 'मेष'],
  SERPENT: ['Serpent', 'सर्प'], DOG: ['Dog', 'श्वान'], CAT: ['Cat', 'मार्जार'],
  RAT: ['Rat', 'मूषक'], COW: ['Cow', 'गौ'], BUFFALO: ['Buffalo', 'महिष'],
  TIGER: ['Tiger', 'व्याघ्र'], DEER: ['Deer', 'मृग'], MONKEY: ['Monkey', 'वानर'],
  MONGOOSE: ['Mongoose', 'नकुल'], LION: ['Lion', 'सिंह'],
};

/**
 * The engine's dosha `reason` strings form a small fixed set (plus a couple of
 * parameterised variants). Map them to Hindi by pattern; unknown strings fall
 * back to English so nothing ever breaks on a backend change.
 */
const DOSHA_REASON_HI: Array<[RegExp, (m: RegExpMatchArray) => string]> = [
  [/^Different Nadi — no dosha$/, () => 'अलग-अलग नाड़ी — कोई दोष नहीं'],
  [/^Cancelled: same Rashi, different Nakshatra$/, () => 'निरस्त: एक ही राशि, अलग नक्षत्र'],
  [/^Cancelled: same Nakshatra, different Pada$/, () => 'निरस्त: एक ही नक्षत्र, अलग चरण'],
  [/^Same Nadi \((.+)\) — dosha applies$/, m => `एक ही नाड़ी (${m[1]}) — दोष लागू`],
  [/^Rashi distance not a dosha pair$/, () => 'राशियों की दूरी दोष-युग्म नहीं है'],
  [/^Cancelled: both signs share lord (\w+)$/,
    m => `निरस्त: दोनों राशियों का स्वामी एक ही ग्रह (${GRAHA_HI[m[1]] ?? m[1]}) है`],
  [/^Cancelled: lords (\w+) and (\w+) are mutual friends$/,
    m => `निरस्त: राशि स्वामी ${GRAHA_HI[m[1]] ?? m[1]} और ${GRAHA_HI[m[2]] ?? m[2]} परस्पर मित्र हैं`],
  [/^Bhakoot dosha applies$/, () => 'भकूट दोष लागू है'],
];

@Injectable({ providedIn: 'root' })
export class I18nService {
  readonly lang = signal<Lang>(initialLang());

  setLang(lang: Lang): void {
    this.lang.set(lang);
    try { localStorage.setItem(STORAGE_KEY, lang); } catch { /* private mode */ }
    document.documentElement.lang = lang;
  }

  /** Translate a UI string; `{param}` placeholders are substituted. */
  t(key: string, params?: Record<string, string | number>): string {
    let msg = MESSAGES[this.lang()][key] ?? MESSAGES.en[key] ?? key;
    if (params) {
      for (const [k, v] of Object.entries(params)) {
        msg = msg.replace(`{${k}}`, String(v));
      }
    }
    return msg;
  }

  rashi(name: string): string {
    return this.lang() === 'hi' ? (RASHI_HI[name] ?? name) : name;
  }

  nakshatra(name: string): string {
    return this.lang() === 'hi' ? (NAKSHATRA_HI[name] ?? name) : name;
  }

  koota(name: string): string {
    return this.lang() === 'hi' ? (KOOTA_HI[name] ?? name) : name;
  }

  // ---- Kundli-attribute vocabularies (backend emits stable codes) ----

  private vocab(map: Record<string, [string, string]>, code: string): string {
    const entry = map[code];
    if (!entry) return code;
    return this.lang() === 'hi' ? entry[1] : entry[0];
  }

  gana(code: string): string { return this.vocab(GANA_V, code); }
  nadi(code: string): string { return this.vocab(NADI_V, code); }
  varna(code: string): string { return this.vocab(VARNA_V, code); }
  vashya(code: string): string { return this.vocab(VASHYA_V, code); }
  yoni(code: string): string { return this.vocab(YONI_V, code); }

  /**
   * Translate a single koota's per-person value (e.g. "Rakshasa", "Horse",
   * "Aadi", "Mesha") for display beside the bar. The engine emits these as
   * title-case Sanskrit; route each koota to its matching vocabulary so
   * Hindi renders too. Unknown koota types (Tara counts) fall back to the
   * raw value, which is already readable.
   */
  kootaValue(code: KootaScore['code'], value: string): string {
    if (!value) return '—';
    const upper = value.toUpperCase();
    switch (code) {
      case 'GANA':   return this.vocab(GANA_V, upper);
      case 'NADI':   return this.vocab(NADI_V, upper);
      case 'VARNA':  return this.vocab(VARNA_V, upper);
      case 'VASHYA': return this.vocab(VASHYA_V, upper);
      case 'YONI':   return this.vocab(YONI_V, upper);
      case 'BHAKOOT':      return this.rashi(value);
      case 'GRAHA_MAITRI': return this.graha(value);
      default:       return value; // Tara and anything new: raw title-case.
    }
  }

  /** Planet name for display: Hindi from the fixed table, else title-case the code. */
  graha(name: string): string {
    if (this.lang() === 'hi') return GRAHA_HI[name] ?? name;
    return name.charAt(0).toUpperCase() + name.slice(1).toLowerCase();
  }

  doshaName(name: string): string {
    return this.lang() === 'hi' ? (KOOTA_HI[name] ?? name) : name;
  }

  doshaReason(reason: string): string {
    if (this.lang() !== 'hi') return reason;
    for (const [re, fn] of DOSHA_REASON_HI) {
      const m = reason.match(re);
      if (m) return fn(m);
    }
    return reason;
  }

  /**
   * Verdict rendered client-side (same thresholds as the backend engine:
   * 18 / 25 / 33 with effective-dosha caveat) so it follows the UI language.
   */
  verdict(result: AshtakootResult): string {
    const total = result.totalPoints;
    let band: string;
    if (total < 18) band = this.t('verdict.notRecommended');
    else if (total < 25) band = this.t('verdict.acceptable');
    else if (total < 33) band = this.t('verdict.good');
    else band = this.t('verdict.excellent');

    const effective = result.doshas.filter(d => d.effective).map(d => this.doshaName(d.name));
    // Same "%.1f/%.0f" shape as the backend's verdict string.
    const score = `${total.toFixed(1)}/${result.maxPoints.toFixed(0)}`;
    if (effective.length === 0) {
      return `${score} — ${band} (${this.t('verdict.noDosha')})`;
    }
    const joiner = this.lang() === 'hi' ? ' और ' : ' & ';
    return `${score} — ${band}, ${this.t('verdict.doshaApplies', { doshas: effective.join(joiner) })}`;
  }
}

function initialLang(): Lang {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored === 'en' || stored === 'hi') return stored;
  } catch { /* private mode */ }
  return typeof navigator !== 'undefined' && navigator.language?.startsWith('hi') ? 'hi' : 'en';
}
