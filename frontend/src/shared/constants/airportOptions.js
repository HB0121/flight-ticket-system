export const AIRPORT_OPTIONS = [
  {
    code: 'CKG',
    cityCode: 'CKG',
    cityNameZh: '重庆',
    cityNameEn: 'Chongqing',
    airportNameZh: '重庆江北',
    airportNameEn: 'Chongqing Jiangbei'
  },
  {
    code: 'PEK',
    cityCode: 'BJS',
    cityNameZh: '北京',
    cityNameEn: 'Beijing',
    airportNameZh: '北京首都',
    airportNameEn: 'Beijing Capital'
  },
  {
    code: 'PKX',
    cityCode: 'BJS',
    cityNameZh: '北京',
    cityNameEn: 'Beijing',
    airportNameZh: '北京大兴',
    airportNameEn: 'Beijing Daxing'
  },
  {
    code: 'PVG',
    cityCode: 'SHA',
    cityNameZh: '上海',
    cityNameEn: 'Shanghai',
    airportNameZh: '上海浦东',
    airportNameEn: 'Shanghai Pudong'
  },
  {
    code: 'SHA',
    cityCode: 'SHA',
    cityNameZh: '上海',
    cityNameEn: 'Shanghai',
    airportNameZh: '上海虹桥',
    airportNameEn: 'Shanghai Hongqiao'
  },
  {
    code: 'CAN',
    cityCode: 'CAN',
    cityNameZh: '广州',
    cityNameEn: 'Guangzhou',
    airportNameZh: '广州白云',
    airportNameEn: 'Guangzhou Baiyun'
  },
  {
    code: 'SZX',
    cityCode: 'SZX',
    cityNameZh: '深圳',
    cityNameEn: 'Shenzhen',
    airportNameZh: '深圳宝安',
    airportNameEn: 'Shenzhen Baoan'
  },
  {
    code: 'TFU',
    cityCode: 'CTU',
    cityNameZh: '成都',
    cityNameEn: 'Chengdu',
    airportNameZh: '成都天府',
    airportNameEn: 'Chengdu Tianfu'
  },
  {
    code: 'CTU',
    cityCode: 'CTU',
    cityNameZh: '成都',
    cityNameEn: 'Chengdu',
    airportNameZh: '成都双流',
    airportNameEn: 'Chengdu Shuangliu'
  },
  {
    code: 'XIY',
    cityCode: 'SIA',
    cityNameZh: '西安',
    cityNameEn: 'Xi an',
    airportNameZh: '西安咸阳',
    airportNameEn: 'Xi an Xianyang'
  },
  {
    code: 'SHE',
    cityCode: 'SHE',
    cityNameZh: '沈阳',
    cityNameEn: 'Shenyang',
    airportNameZh: '沈阳桃仙',
    airportNameEn: 'Shenyang Taoxian'
  },
  {
    code: 'KHN',
    cityCode: 'KHN',
    cityNameZh: '南昌',
    cityNameEn: 'Nanchang',
    airportNameZh: '南昌昌北',
    airportNameEn: 'Nanchang Changbei'
  },
  {
    code: 'SWA',
    cityCode: 'SWA',
    cityNameZh: '汕头',
    cityNameEn: 'Shantou',
    airportNameZh: '揭阳潮汕',
    airportNameEn: 'Jieyang Chaoshan'
  },
  {
    code: 'TSN',
    cityCode: 'TSN',
    cityNameZh: '天津',
    cityNameEn: 'Tianjin',
    airportNameZh: '天津滨海',
    airportNameEn: 'Tianjin Binhai'
  },
  {
    code: 'NNG',
    cityCode: 'NNG',
    cityNameZh: '南宁',
    cityNameEn: 'Nanning',
    airportNameZh: '南宁吴圩',
    airportNameEn: 'Nanning Wuxu'
  },
  {
    code: 'UYN',
    cityCode: 'UYN',
    cityNameZh: '榆林',
    cityNameEn: 'Yulin',
    airportNameZh: '榆林榆阳',
    airportNameEn: 'Yulin Yuyang'
  }
]

export function findAirportOption(code) {
  const normalizedCode = String(code ?? '').trim().toUpperCase()
  return AIRPORT_OPTIONS.find(option => option.code === normalizedCode) ?? null
}

export function buildAirportOptionLabel(code, locale = 'zh-CN') {
  const airport = findAirportOption(code)
  if (!airport) {
    return String(code ?? '').trim() || '-'
  }

  return locale === 'zh-CN'
    ? `${airport.airportNameZh} ${airport.code}`
    : `${airport.airportNameEn} ${airport.code}`
}
