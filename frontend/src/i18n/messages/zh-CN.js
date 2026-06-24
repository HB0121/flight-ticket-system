export default {
  common: {
    locales: {
      zhCN: '中文',
      enUS: 'English'
    },
    actions: {
      searchFlights: '查询航班',
      refresh: '刷新',
      remove: '移除',
      favorite: '收藏',
      unfavorite: '取消收藏',
    },
    labels: {
      source: '来源',
      departure: '出发',
      arrival: '到达',
      route: '航线',
      airports: '机场',
      travelDate: '出行日期',
      searchedAt: '搜索时间'
    },
    status: {
      loadingFlights: '正在加载航班...',
      loadingFavorites: '正在加载收藏...',
      loadingHistory: '正在加载搜索历史...',
      loadingPriceHistory: '正在加载价格历史...'
    },
  },
  layout: {
    shell: {
      phase: '阶段 1',
      userTitle: '用户航班',
      adminTitle: '管理采集'
    },
    nav: {
      flights: '航班',
      favorites: '收藏',
      history: '历史',
      crawlJobs: '采集任务',
      dataSources: '数据源'
    }
  },
  auth: {
    brand: '航班查询平台',
    login: {
      title: '登录',
      subtitle: '登录后继续查询航班，或创建新的演示账号。',
      submit: '登录',
      invalidSession: '登录成功，但会话数据无效。',
      registerInvalidSession: '注册成功，但会话数据无效。',
      success: '登录成功，正在跳转到航班页。',
      error: '登录失败，请稍后重试。'
    },
    register: {
      submit: '创建账号',
      success: '注册成功，已自动登录。',
      error: '注册失败，请稍后重试。'
    },
    fields: {
      username: '用户名',
      password: '密码',
      nickname: '昵称（可选）'
    },
    placeholders: {
      username: '请输入用户名',
      password: '请输入密码',
      nickname: '留空时默认使用用户名'
    },
    switcher: {
      login: '登录',
      register: '注册'
    },
    footnote: {
      loginPrompt: '还没有账号？',
      loginAction: '去注册',
      registerPrompt: '已有账号？',
      registerAction: '返回登录'
    },
    validation: {
      missingCredentials: '请输入用户名和密码。',
      shortPassword: '注册密码至少 4 位。'
    },
    logout: {
      button: '退出登录',
      confirm: '确定要退出登录吗？',
      success: '已退出登录',
      error: '退出请求失败，但本地会话已清除'
    }
  },
  flights: {
    console: {
      eyebrow: '\u9636\u6bb5 1',
      title: '\u673a\u7968\u67e5\u8be2\u7cfb\u7edf',
      subtitle: '\u57fa\u4e8e AeroDataBox \u7684\u822a\u73ed\u6570\u636e\u67e5\u8be2\u4e0e AI \u51fa\u884c\u5efa\u8bae\u5e73\u53f0',
      badges: {
        dataSource: '数据来源：AeroDataBox',
        mode: '模式：本地 MySQL 查询'
      }
    },
    sync: {
      eyebrow: '\u822a\u73ed\u540c\u6b65',
      title: '\u540c\u6b65\u822a\u73ed\u6570\u636e',
      description: '\u901a\u8fc7\u540e\u7aef\u89e6\u53d1\u540c\u6b65\uff0c\u7531 crawler \u5199\u5165 MySQL\uff0c\u518d\u57fa\u4e8e\u672c\u5730\u6570\u636e\u67e5\u8be2\u5c55\u793a\u3002',
      form: {
        airportCode: '\u673a\u573a\u4ee3\u7801',
        date: '\u540c\u6b65\u65e5\u671f'
      },
      placeholders: {
        airportCode: 'CKG',
        date: 'YYYY-MM-DD'
      },
      actions: {
        syncDate: '\u540c\u6b65\u6307\u5b9a\u65e5\u671f\u822a\u73ed',
        syncToday: '\u540c\u6b65\u4eca\u65e5\u822a\u73ed'
      },
      messages: {
        success: '\u822a\u73ed\u540c\u6b65\u5b8c\u6210\u3002',
        failed: '\u822a\u73ed\u540c\u6b65\u5931\u8d25\u3002'
      }
    },
    syncResult: {
      eyebrow: '\u540c\u6b65\u7ed3\u679c',
      title: '\u6700\u8fd1\u4e00\u6b21\u540c\u6b65\u72b6\u6001',
      empty: {
        title: '\u6682\u672a\u6267\u884c\u540c\u6b65',
        description: '\u5148\u6267\u884c\u4e00\u6b21\u540c\u6b65\uff0c\u8fd9\u91cc\u4f1a\u5c55\u793a\u540e\u7aef\u8fd4\u56de\u7684\u4efb\u52a1\u7ed3\u679c\u3002',
        summary: '\u7b49\u5f85\u7b2c\u4e00\u6b21\u540e\u7aef\u540c\u6b65\u6267\u884c\u3002'
      },
      summary: {
        success: '\u540e\u7aef\u540c\u6b65\u5df2\u5b8c\u6210\uff0c\u5e76\u5df2\u5237\u65b0\u672c\u5730\u822a\u73ed\u67e5\u8be2\u7ed3\u679c\u3002',
        failed: '\u540e\u7aef\u540c\u6b65\u5931\u8d25\uff0c\u8bf7\u67e5\u770b\u4e0b\u65b9\u9519\u8bef\u4fe1\u606f\u3002',
        empty: '\u540c\u6b65\u5df2\u5b8c\u6210\uff0c\u4f46\u5f53\u524d\u672a\u8fd4\u56de\u822a\u73ed\u6570\u636e\u3002'
      },
      fields: {
        successCount: '\u6210\u529f\u6570\u91cf',
        failedCount: '\u5931\u8d25\u6570\u91cf',
        source: '\u6570\u636e\u6765\u6e90',
        requestParams: '\u8bf7\u6c42\u53c2\u6570',
        startedAt: '\u5f00\u59cb\u65f6\u95f4',
        finishedAt: '\u7ed3\u675f\u65f6\u95f4',
        errorMessage: '\u9519\u8bef\u4fe1\u606f'
      }
    },
    search: {
      eyebrow: '\u822a\u73ed\u67e5\u8be2',
      title: '\u67e5\u8be2\u672c\u5730\u822a\u73ed\u5feb\u7167',
      description: '\u4fdd\u7559\u539f\u6709\u67e5\u8be2\u6d41\u7a0b\uff0c\u53ea\u67e5\u8be2 Spring Boot \u63d0\u4f9b\u7684\u672c\u5730 MySQL \u822a\u73ed\u6570\u636e\u3002'
    },
    eyebrow: '用户航班查询',
    title: '查询当前航班快照',
    subtitle: '当前 phase-1 页面聚焦于搜索、详情查看和价格历史，不把管理端或 AI 逻辑混入用户流程。',
    filters: {
      from: '出发地',
      to: '目的地',
      date: '日期',
      source: '来源'
    },
    placeholders: {
      from: '出发城市',
      to: '到达城市',
      date: '选择出发日期',
      source: '可选来源筛选'
    },
    table: {
      title: '匹配航班',
      empty: '当前筛选条件下没有找到航班。',
      unknownAirline: '未知航司',
      results: '当前查询共 {count} 条结果。',
      columns: {
        flight: '航班',
        route: '航线',
        departure: '出发',
        arrival: '到达',
        price: '价格',
        seats: '座位',
        source: '来源'
      }
    },
    detail: {
      eyebrow: '已选航班',
      airline: '航司',
      route: '航线',
      airports: '机场',
      departure: '出发',
      arrival: '到达',
      seatsLeft: '余票',
      source: '来源',
      collected: '采集时间'
    },
    history: {
      title: '价格历史',
      loading: '正在加载价格历史...',
      empty: '该航班暂时还没有价格历史。',
      waiting: '多次采集到同一航班后，这里会显示价格历史。',
      unknownTime: '未知时间',
      summary: '已记录 {count} 次采集，价格区间 ¥{lowest} - ¥{highest}。'
    },
    errors: {
      loadFailed: '加载航班失败，请稍后重试。',
      partialHistory: '航班详情已加载，但价格历史暂时不可用。'
    },
    favorite: {
      added: '已收藏',
      removed: '已取消收藏',
      failed: '操作失败，请重试'
    },
    results: {
      eyebrow: '\u822a\u73ed\u7ed3\u679c',
      title: '\u822a\u73ed\u67e5\u8be2\u7ed3\u679c',
      description: '\u4e0b\u65b9\u7ed3\u679c\u8868\u5c55\u793a\u7684\u662f\u672c\u5730\u6570\u636e\u5e93\u4e2d\u7684\u822a\u73ed\u6570\u636e\u3002',
      emptyHint: '\u5f53\u524d\u8fd8\u6ca1\u6709\u672c\u5730\u6570\u636e\uff0c\u5efa\u8bae\u5148\u6267\u884c\u540c\u6b65\u3002',
      emptyTitle: '\u672a\u67e5\u8be2\u5230\u672c\u5730\u822a\u73ed',
      emptyDescription: '\u8bf7\u5148\u540c\u6b65\u6307\u5b9a\u673a\u573a\u548c\u65e5\u671f\u7684\u6570\u636e\uff0c\u518d\u91cd\u65b0\u67e5\u8be2\u672c\u5730\u822a\u73ed\u5217\u8868\u3002'
    }
  },
  favorites: {
    eyebrow: '用户资料',
    title: '收藏',
    subtitle: '查看你已经从搜索结果中收藏的航班。',
    empty: '还没有收藏航班。',
    errors: {
      loadFailed: '当前无法加载收藏列表。',
      removeFailed: '当前无法移除这条收藏。'
    }
  },
  history: {
    eyebrow: '用户资料',
    title: '搜索历史',
    subtitle: '最近的航班搜索会从当前查询流程中自动记录。',
    empty: '还没有搜索历史。',
    errors: {
      loadFailed: '当前无法加载搜索历史。'
    }
  },
  admin: {
    crawlJobs: {
      eyebrow: '管理采集任务',
      title: '通过管理边界运行采集任务',
      subtitle: '该页面将 phase-1 的采集管理限制在任务创建和最近任务查看。',
      form: {
        source: '来源',
        from: '出发地',
        to: '目的地',
        date: '日期',
        adults: '乘客数',
        maxResults: '最大结果数'
      },
      placeholders: {
        from: '上海',
        to: '北京'
      },
      actions: {
        submit: '创建采集任务',
        submitting: '提交中...',
        refresh: '刷新',
        refreshing: '刷新中...'
      },
      recentJobs: {
        title: '最近任务',
        empty: '暂时还没有采集任务。',
        columns: {
          id: '编号',
          source: '来源',
          status: '状态',
          success: '成功',
          failed: '失败',
          started: '开始时间'
        }
      },
      errors: {
        noConfiguredSource: '当前没有可用的远程数据源配置。',
        loadJobsFailed: '当前无法加载采集任务。',
        loadStatusesFailed: '当前无法加载数据源状态。',
        createFailed: '当前无法创建采集任务。'
      }
    },
    dataSources: {
      eyebrow: '管理数据源',
      title: '数据源状态',
      subtitle: '该页面显示真实远程数据源是否已正确配置，不再暴露任何兜底数据源。',
      actions: {
        refresh: '刷新',
        refreshing: '刷新中...'
      },
      badges: {
        configured: '已配置',
        notConfigured: '未配置'
      },
      errors: {
        loadFailed: '当前无法加载数据源状态。'
      }
    }
  }
}
