declare function moment(inp?: moment.MomentInput, format?: moment.MomentFormatSpecification, strict?: boolean): moment.Moment;
declare function moment(inp?: moment.MomentInput, format?: moment.MomentFormatSpecification, language?: string, strict?: boolean): moment.Moment;

declare namespace moment {
  type RelativeTimeKey = 's' | 'm' | 'mm' | 'h' | 'hh' | 'd' | 'dd' | 'M' | 'MM' | 'y' | 'yy';
  type CalendarKey = 'sameDay' | 'nextDay' | 'lastDay' | 'nextWeek' | 'lastWeek' | 'sameElse' | string;
  type LongDateFormatKey = 'LTS' | 'LT' | 'L' | 'LL' | 'LLL' | 'LLLL' | 'lts' | 'lt' | 'l' | 'll' | 'lll' | 'llll' | string; // I suspect some of these to be errors, but it is the Moment implementation that has given me all these values, they have been concretely used where a LongDateFormatKey is expected.

  interface Locale {
    calendar(key?: CalendarKey, m?: Moment, now?: Moment): string | undefined;

    longDateFormat(key: LongDateFormatKey): string | undefined;
    invalidDate(): string | undefined;
    ordinal(n: number): string;

    preparse(inp: string): string;
    postformat(inp?: string): string | undefined;
    relativeTime(n: number, withoutSuffix: boolean,
                 key: RelativeTimeKey, isFuture: boolean): string;
    pastFuture(diff: number, absRelTime: string): string;
    set(config: Object): void;

    months(): string | string[] | StandaloneFormatSpec | undefined;
    months(m: Moment, format?: string): string | undefined;
    monthsShort(): string | string[] | StandaloneFormatSpec | undefined;
    monthsShort(m: Moment, format?: string): string | undefined;
    monthsParse(monthName: string, format?: string, strict?: boolean): number | undefined;
    monthsRegex(strict?: boolean): RegExp;
    monthsShortRegex(strict?: boolean): RegExp;

    week(m: Moment): number;
    firstDayOfYear(): number;
    firstDayOfWeek(): number;

    weekdays(): string | string[] | StandaloneFormatSpec | undefined;
    weekdays(m: Moment, format?: string): string | undefined;
    weekdaysMin(): string | string[] | StandaloneFormatSpec | undefined;
    weekdaysMin(m: Moment): string | string[] | undefined;
    weekdaysMin(m: Moment, format: string): string | undefined;
    weekdaysShort(): string | string[] | StandaloneFormatSpec | undefined;
    weekdaysShort(m: Moment): string | undefined;
    weekdaysShort(m: Moment, format: string): string | undefined;
    weekdaysParse(weekdayName: string, format?: string, strict?: boolean): number | undefined;
    weekdaysRegex(strict?: boolean): RegExp;
    weekdaysShortRegex(strict?: boolean): RegExp;
    weekdaysMinRegex(strict?: boolean): RegExp;

    isPM(input: string): boolean;
    meridiem(hour: number, minute: number, isLower: boolean): string;
  }

  interface StandaloneFormatSpec {
    format: string[];
    standalone: string[];
    isFormat?: RegExp;
  }

  interface WeekSpec {
    dow: number;
    doy: number;
  }

  type CalendarSpecVal = string | ((m?: MomentInput, now?: Moment) => string);
  interface CalendarSpec {
    sameDay?: CalendarSpecVal;
    nextDay?: CalendarSpecVal;
    lastDay?: CalendarSpecVal;
    nextWeek?: CalendarSpecVal;
    lastWeek?: CalendarSpecVal;
    sameElse?: CalendarSpecVal;

    // any additional properties might be used with moment.calendarFormat
    [x: string]: CalendarSpecVal | undefined;
  }

  type RelativeTimeSpecVal = (
    string |
    ((n: number, withoutSuffix: boolean,
      key: RelativeTimeKey, isFuture: boolean) => string)
  );
  type RelativeTimeFuturePastVal = string | ((relTime: string) => string);

  interface RelativeTimeSpec {
    future: RelativeTimeFuturePastVal;
    past: RelativeTimeFuturePastVal;
    s: RelativeTimeSpecVal;
    m: RelativeTimeSpecVal;
    mm: RelativeTimeSpecVal;
    h: RelativeTimeSpecVal;
    hh: RelativeTimeSpecVal;
    d: RelativeTimeSpecVal;
    dd: RelativeTimeSpecVal;
    M: RelativeTimeSpecVal;
    MM: RelativeTimeSpecVal;
    y: RelativeTimeSpecVal;
    yy: RelativeTimeSpecVal;
  }

  interface LongDateFormatSpec {
    LTS: string;
    LT: string;
    L: string;
    LL: string;
    LLL: string;
    LLLL: string;

    // lets forget for a sec that any upper/lower permutation will also work
    lts?: string;
    lt?: string;
    l?: string;
    ll?: string;
    lll?: string;
    llll?: string;
  }

  type MonthWeekdayFn = (momentToFormat?: Moment, format?: string) => string;
  type WeekdaySimpleFn = (momentToFormat?: Moment) => string;

  interface LocaleSpecification {
    months?: string[] | StandaloneFormatSpec | MonthWeekdayFn;
    monthsShort?: string[] | StandaloneFormatSpec | MonthWeekdayFn;

    weekdays?: string[] | StandaloneFormatSpec | MonthWeekdayFn;
    weekdaysShort?: string[] | StandaloneFormatSpec | WeekdaySimpleFn;
    weekdaysMin?: string[] | StandaloneFormatSpec | WeekdaySimpleFn;

    meridiemParse?: RegExp;
    meridiem?: (hour: number, minute:number, isLower: boolean) => string;

    isPM?: (input: string) => boolean;

    longDateFormat?: LongDateFormatSpec;
    calendar?: CalendarSpec;
    relativeTime?: RelativeTimeSpec;
    invalidDate?: string;
    ordinal?: (n: number) => string;
    ordinalParse?: RegExp;

    week?: WeekSpec;

    // Allow anything: in general any property that is passed as locale spec is
    // put in the locale object so it can be used by locale functions
    [x: string]: any;
  }

  interface MomentObjectOutput {
    years: number;
    /* One digit */
    months: number;
    /* Day of the month */
    date: number;
    hours: number;
    minutes: number;
    seconds: number;
    milliseconds: number;
  }

  interface Duration {
    humanize(withSuffix?: boolean): string | undefined;

    abs(): Duration;

    as(units: unitOfTime.Base): number;
    get(units: unitOfTime.Base): number;

    milliseconds(): number;
    asMilliseconds(): number;

    seconds(): number;
    asSeconds(): number;

    minutes(): number;
    asMinutes(): number;

    hours(): number;
    asHours(): number;

    days(): number;
    asDays(): number;

    weeks(): number;
    asWeeks(): number;

    months(): number;
    asMonths(): number;

    years(): number;
    asYears(): number;

    add(inp?: DurationInputArg1, unit?: DurationInputArg2): Duration;
    subtract(inp?: DurationInputArg1, unit?: DurationInputArg2): Duration;

    locale(): string | undefined;
    locale(locale: LocaleSpecifier): Duration;
    localeData(): Locale | undefined;

    toISOString(): string | undefined;
    toJSON(): string | null;

    /**
     * @deprecated since version 2.8.0
     */
    lang(locale: LocaleSpecifier): Moment | Duration;
    /**
     * @deprecated since version 2.8.0
     */
    lang(): Locale | undefined;
    /**
     * @deprecated
     */
    toIsoString(): string;
  }

  interface MomentRelativeTime {
    future: any;
    past: any;
    s: any;
    m: any;
    mm: any;
    h: any;
    hh: any;
    d: any;
    dd: any;
    M: any;
    MM: any;
    y: any;
    yy: any;
  }

  interface MomentLongDateFormat {
    L: string;
    LL: string;
    LLL: string;
    LLLL: string;
    LT: string;
    LTS: string;

    l?: string;
    ll?: string;
    lll?: string;
    llll?: string;
    lt?: string;
    lts?: string;
  }

  interface MomentParsingFlags {
    empty: boolean;
    unusedTokens: string[] | undefined;
    unusedInput: string[] | undefined;
    overflow: number;
    charsLeftOver: number | undefined;
    nullInput: boolean | undefined;
    invalidMonth: string | null;
    invalidFormat: boolean;
    userInvalidated: boolean;
    iso: boolean;
    parsedDateParts: any[];
    meridiem: string | undefined | null;
  }

  interface MomentParsingFlagsOpt {
    empty?: boolean;
    unusedTokens?: string[];
    unusedInput?: string[];
    overflow?: number;
    charsLeftOver?: number;
    nullInput?: boolean;
    invalidMonth?: string | null;
    invalidFormat?: boolean;
    userInvalidated?: boolean;
    iso?: boolean;
    parsedDateParts?: any[];
    meridiem?: string | null;
  }

  interface MomentBuiltinFormat {
    __momentBuiltinFormatBrand: any;
  }

  type MomentFormatSpecification = string | MomentBuiltinFormat | (string | MomentBuiltinFormat)[];

  namespace unitOfTime {
    type Base = (
      "year" | "years" | "y" |
      "month" | "months" | "M" |
      "week" | "weeks" | "w" |
      "day" | "days" | "d" |
      "hour" | "hours" | "h" |
      "minute" | "minutes" | "m" |
      "second" | "seconds" | "s" |
      "millisecond" | "milliseconds" | "ms"
    );

    type _quarter = "quarter" | "quarters" | "Q";
    type _isoWeek = "isoWeek" | "isoWeeks" | "W";
    type _date = "date" | "dates" | "D";
    type DurationConstructor = Base | _quarter;

    type DurationAs = Base;

    type StartOf = Base | _quarter | _isoWeek | _date;

    type Diff = Base | _quarter;

    type MomentConstructor = Base | _date;

    type All = Base | _quarter | _isoWeek | _date |
      "weekYear" | "weekYears" | "gg" |
      "isoWeekYear" | "isoWeekYears" | "GG" |
      "dayOfYear" | "dayOfYears" | "DDD" |
      "weekday" | "weekdays" | "e" |
      "isoWeekday" | "isoWeekdays" | "E";
  }

  interface MomentInputObject {
    years?: number;
    year?: number;
    y?: number;

    months?: number;
    month?: number;
    M?: number;

    days?: number;
    day?: number;
    d?: number;

    dates?: number;
    date?: number;
    D?: number;

    hours?: number;
    hour?: number;
    h?: number;

    minutes?: number;
    minute?: number;
    m?: number;

    seconds?: number;
    second?: number;
    s?: number;

    milliseconds?: number;
    millisecond?: number;
    ms?: number;
  }

  interface DurationInputObject extends MomentInputObject {
    quarters?: number;
    quarter?: number;
    Q?: number;
  }

  interface MomentSetObject extends MomentInputObject {
    weekYears?: number;
    weekYear?: number;
    gg?: number;

    isoWeekYears?: number;
    isoWeekYear?: number;
    GG?: number;

    quarters?: number;
    quarter?: number;
    Q?: number;

    weeks?: number;
    week?: number;
    w?: number;

    isoWeeks?: number;
    isoWeek?: number;
    W?: number;

    dayOfYears?: number;
    dayOfYear?: number;
    DDD?: number;

    weekdays?: number;
    weekday?: number;
    e?: number;

    isoWeekdays?: number;
    isoWeekday?: number;
    E?: number;
  }

  interface FromTo {
    from: MomentInput;
    to: MomentInput;
  }

  type MomentInput = Moment | Date | string | number | (number | string)[] | MomentInputObject | null | undefined;
  type DurationInputArg1 = Duration | number | string | FromTo | DurationInputObject | null | undefined;
  type DurationInputArg2 = unitOfTime.DurationConstructor;
  type LocaleSpecifier = string | Moment | Duration | string[];

  interface MomentCreationData {
    input: string | number | Date | undefined | Moment | (number | string)[] | MomentInputObject;
    format?: Function | string | (Function | string)[] | MomentBuiltinFormat;
    locale?: Locale;
    isUTC?: boolean;
    strict?: boolean;
  }

  interface Moment {
    format(format?: string): string | undefined;

    startOf(unitOfTime: unitOfTime.StartOf): Moment;
    endOf(unitOfTime: unitOfTime.StartOf): Moment | undefined;

    add(amount?: DurationInputArg1, unit?: DurationInputArg2): Moment;
    /**
     * @deprecated reverse syntax
     */
    add(unit: unitOfTime.DurationConstructor, amount: number|string): Moment;

    subtract(amount?: DurationInputArg1, unit?: DurationInputArg2): Moment;
    /**
     * @deprecated reverse syntax
     */
    subtract(unit: unitOfTime.DurationConstructor, amount: number|string): Moment;

    calendar(time?: MomentInput, formats?: CalendarSpec): string | undefined;

    clone(): Moment;

    /**
     * @return Unix timestamp in milliseconds
     */
    valueOf(): number;

    // current date/time in local mode
    local(keepLocalTime?: boolean): Moment;
    isLocal(): boolean;

    // current date/time in UTC mode
    utc(keepLocalTime?: boolean): Moment;
    isUTC(): boolean | undefined;
    /**
     * @deprecated use isUTC
     */
    isUtc(): boolean | undefined;

    parseZone(): Moment;
    isValid(): boolean;
    invalidAt(): number | undefined;

    hasAlignedHourOffset(other?: MomentInput): boolean;

    creationData(): MomentCreationData;
    parsingFlags(): MomentParsingFlagsOpt;

    year(y: number): Moment;
    year(y: undefined): number;
    year(): number;
    /**
     * @deprecated use year(y)
     */
    years(y: number): Moment;
    /**
     * @deprecated use year()
     */
    years(): number;
    quarter(): number;
    quarter(q: undefined): number;
    quarter(q: number): Moment;
    quarters(): number;
    quarters(q: number): Moment;
    month(M: number|string): Moment;
    month(M: undefined): number;
    month(): number;
    /**
     * @deprecated use month(M)
     */
    months(M: number|string): Moment | undefined;
    /**
     * @deprecated use month()
     */
    months(): number | string | Date | MomentInputObject;
    day(d: number|string): Moment;
    day(d: undefined | null): number;
    day(): number;
    days(d: number|string): Moment;
    days(): number;
    date(d: number): Moment;
    date(d: undefined): number;
    date(): number;
    /**
     * @deprecated use date(d)
     */
    dates(d: number): Moment;
    /**
     * @deprecated use date()
     */
    dates(): number;
    hour(h: number): Moment;
    hour(h: undefined): number;
    hour(): number;
    hours(h: number): Moment;
    hours(): number;
    minute(m: number): Moment;
    minute(m: undefined): number;
    minute(): number;
    minutes(m: number): Moment;
    minutes(): number;
    second(s: number): Moment;
    second(s: undefined): number;
    second(): number;
    seconds(s: number): Moment;
    seconds(): number;
    millisecond(ms: number): Moment;
    millisecond(ms: undefined): number;
    millisecond(): number;
    milliseconds(ms: number): Moment;
    milliseconds(): number;
    weekday(): number;
    weekday(d: undefined): number;
    weekday(d: number): Moment;
    isoWeekday(): number;
    isoWeekday(d: undefined): number;
    isoWeekday(d: number|string): Moment | number | undefined;
    weekYear(): number;
    weekYear(d: undefined): number;
    weekYear(d: number): Moment | undefined;
    isoWeekYear(): number;
    isoWeekYear(d: undefined): number;
    isoWeekYear(d: number): Moment;
    week(): number;
    week(d: undefined): number;
    week(d: number): Moment;
    weeks(): number;
    weeks(d: number): Moment;
    isoWeek(): number;
    isoWeek(d: undefined): number;
    isoWeek(d: number): Moment;
    isoWeeks(): number;
    isoWeeks(d: number): Moment;
    weeksInYear(): number;
    isoWeeksInYear(): number;
    dayOfYear(): number;
    dayOfYear(d: undefined): number;
    dayOfYear(d: number): Moment;

    from(inp: MomentInput, suffix?: boolean): string | undefined;
    to(inp: MomentInput, suffix?: boolean): string | undefined;
    fromNow(withoutSuffix?: boolean): string | undefined;
    toNow(withoutPrefix?: boolean): string | undefined;

    diff(b: MomentInput, unitOfTime?: unitOfTime.Diff, precise?: boolean): number;

    toArray(): number[];
    toDate(): Date;
    toISOString(): string | undefined;
    inspect(): string | undefined;
    toJSON(): string | undefined | null;
    unix(): number;

    isLeapYear(): boolean;
    /**
     * @deprecated in favor of utcOffset
     */
    zone(): number;
    zone(b: number|string): Moment;
    utcOffset(): number;
    utcOffset(b: number|string, keepLocalTime?: boolean): Moment;
    daysInMonth(): number;
    isDST(): boolean;

    zoneAbbr(): string;
    zoneName(): string;

    isBefore(inp?: MomentInput, granularity?: unitOfTime.StartOf): boolean;
    isAfter(inp?: MomentInput, granularity?: unitOfTime.StartOf): boolean;
    isSame(inp?: MomentInput, granularity?: unitOfTime.StartOf): boolean;
    isSameOrAfter(inp?: MomentInput, granularity?: unitOfTime.StartOf): boolean;
    isSameOrBefore(inp?: MomentInput, granularity?: unitOfTime.StartOf): boolean;
    isBetween(a: MomentInput, b: MomentInput, granularity?: unitOfTime.StartOf, inclusivity?: "()" | "[)" | "(]" | "[]"): boolean;

    /**
     * @deprecated as of 2.8.0, use locale
     */
    lang(language: LocaleSpecifier): Moment | Duration;
    /**
     * @deprecated as of 2.8.0, use locale
     */
    lang(): Locale | Duration | undefined;

    locale(): string | undefined;
    locale(locale: LocaleSpecifier): Moment;

    localeData(): Locale | undefined;

    /**
     * @deprecated no reliable implementation
     */
    isDSTShifted(): boolean;

    // NOTE(constructor): Same as moment constructor
    /**
     * @deprecated as of 2.7.0, use moment.min/max
     */
    max(inp?: MomentInput, format?: MomentFormatSpecification, strict?: boolean): Moment;
    /**
     * @deprecated as of 2.7.0, use moment.min/max
     */
    max(inp?: MomentInput, format?: MomentFormatSpecification, language?: string, strict?: boolean): Moment;

    // NOTE(constructor): Same as moment constructor
    /**
     * @deprecated as of 2.7.0, use moment.min/max
     */
    min(inp?: MomentInput, format?: MomentFormatSpecification, strict?: boolean): Moment;
    /**
     * @deprecated as of 2.7.0, use moment.min/max
     */
    min(inp?: MomentInput, format?: MomentFormatSpecification, language?: string, strict?: boolean): Moment;

    get(unit: unitOfTime.All): number;
    set(unit: unitOfTime.All, value: number): Moment | number | undefined;
    set(objectLiteral: MomentSetObject): Moment;

    toObject(): MomentObjectOutput;
  }

  export var version: string;
  export var fn: Moment;

  // NOTE(constructor): Same as moment constructor
  export function utc(inp?: MomentInput, format?: MomentFormatSpecification, strict?: boolean): Moment;
  export function utc(inp?: MomentInput, format?: MomentFormatSpecification, language?: string, strict?: boolean): Moment;

  export function unix(timestamp: number): Moment;

  export function invalid(flags?: MomentParsingFlagsOpt): Moment;
  export function isMoment(m: any): m is Moment;
  export function isDate(m: any): m is Date;
  export function isDuration(d: any): d is Duration;

  /**
   * @deprecated in 2.8.0
   */
  export function lang(language?: string): string | undefined;
  /**
   * @deprecated in 2.8.0
   */
  export function lang(language?: string, definition?: Locale): string | undefined;

  export function locale(language?: string): string | undefined;
  export function locale(language?: string[]): string | undefined;
  export function locale(language?: string, definition?: LocaleSpecification | null | undefined): string | undefined;

  export function localeData(key?: string | string[]): Locale | null;

  export function duration(inp?: DurationInputArg1, unit?: DurationInputArg2): Duration;

  // NOTE(constructor): Same as moment constructor
  export function parseZone(inp?: MomentInput, format?: MomentFormatSpecification, strict?: boolean): Moment;
  export function parseZone(inp?: MomentInput, format?: MomentFormatSpecification, language?: string, strict?: boolean): Moment;

  export function months(): (string | undefined)[];
  export function months(index: number): string | undefined;
  export function months(format: string): (string | undefined)[];
  export function months(format: string, index: number): string | undefined;
  export function monthsShort(): (string | undefined)[];
  export function monthsShort(index: number): string | undefined;
  export function monthsShort(format: string): (string | undefined)[];
  export function monthsShort(format: string, index: number): string | undefined;

  export function weekdays(): (string | undefined)[];
  export function weekdays(index: number): string | undefined;
  export function weekdays(format: string): string | undefined;
  export function weekdays(format: string, index: number): string | undefined; //
  export function weekdays(localeSorted: boolean): (string | undefined)[];
  export function weekdays(localeSorted: boolean, index: number): string | undefined; //
  export function weekdays(localeSorted: boolean, format: string): (string | undefined)[]; //
  export function weekdays(localeSorted: boolean, format: string, index: number): string | undefined;
  export function weekdaysShort(): (string | undefined)[]; //
  export function weekdaysShort(index: number): string | undefined;
  export function weekdaysShort(format: string): string | undefined;
  export function weekdaysShort(format: string, index: number): string | undefined; //
  export function weekdaysShort(localeSorted: boolean): (string | undefined)[]; //
  export function weekdaysShort(localeSorted: boolean, index: number): string | undefined; //
  export function weekdaysShort(localeSorted: boolean, format: string): (string | undefined)[];
  export function weekdaysShort(localeSorted: boolean, format: string, index: number): string | undefined; //
  export function weekdaysMin(): (string | undefined)[]; //
  export function weekdaysMin(index: number): string | undefined; //
  export function weekdaysMin(format: string): string | undefined;
  export function weekdaysMin(format: string, index: number): string | undefined;
  export function weekdaysMin(localeSorted: boolean): (string | undefined)[];
  export function weekdaysMin(localeSorted: boolean, index: number): string | undefined; //
  export function weekdaysMin(localeSorted: boolean, format: string): (string | undefined)[];
  export function weekdaysMin(localeSorted: boolean, format: string, index: number): string | undefined; //

  export function min(...moments: MomentInput[]): MomentInput;
  export function max(...moments: MomentInput[]): MomentInput;

  /**
   * Returns unix time in milliseconds. Overwrite for profit.
   */
  export function now(): number;

  export function defineLocale(language: string, localeSpec: LocaleSpecification | null): Locale | null;
  export function updateLocale(language: string, localeSpec: LocaleSpecification | null): Locale | undefined;

  export function locales(): string[];

  export function normalizeUnits(unit: unitOfTime.All): string;
  export function relativeTimeThreshold(threshold: string): number | boolean;
  export function relativeTimeThreshold(threshold: string, limit: number): boolean;
  export function relativeTimeRounding(fn: (num: number) => number): boolean;
  export function relativeTimeRounding(): (num: number) => number;
  export function calendarFormat(m: Moment, now: Moment): string;

  /**
   * Constant used to enable explicit ISO_8601 format parsing.
   */
  export var ISO_8601: MomentBuiltinFormat;

  export var defaultFormat: string;
  export var defaultFormatUtc: string;
}

export = moment;
