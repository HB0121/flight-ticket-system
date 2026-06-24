export default {
  common: {
    locales: {
      zhCN: '中文',
      enUS: 'English'
    },
    actions: {
      searchFlights: 'Search Flights',
      refresh: 'Refresh',
      remove: 'Remove',
      favorite: 'Favorite',
      unfavorite: 'Unfavorite',
    },
    labels: {
      source: 'Source',
      departure: 'Departure',
      arrival: 'Arrival',
      route: 'Route',
      airports: 'Airports',
      travelDate: 'Travel Date',
      searchedAt: 'Searched At'
    },
    status: {
      loadingFlights: 'Loading flights...',
      loadingFavorites: 'Loading favorites...',
      loadingHistory: 'Loading search history...',
      loadingPriceHistory: 'Loading price history...'
    }
  },
  layout: {
    shell: {
      phase: 'Phase 1',
      userTitle: 'User Flights',
      adminTitle: 'Admin Crawl'
    },
    nav: {
      flights: 'Flights',
      favorites: 'Favorites',
      history: 'History',
      crawlJobs: 'Crawl Jobs',
      dataSources: 'Data Sources'
    }
  },
  auth: {
    brand: 'Flight Query Platform',
    login: {
      title: 'Sign in',
      subtitle: 'Sign in to continue to flight search, or create a new demo account.',
      submit: 'Sign in',
      invalidSession: 'Login succeeded but the session payload was invalid.',
      registerInvalidSession: 'Registration succeeded but the session payload was invalid.',
      success: 'Signed in. Redirecting to flight search.',
      error: 'Login failed. Please try again.'
    },
    register: {
      submit: 'Create account',
      success: 'Registration successful. You are now signed in.',
      error: 'Registration failed. Please try again.'
    },
    fields: {
      username: 'Username',
      password: 'Password',
      nickname: 'Nickname (optional)'
    },
    placeholders: {
      username: 'Enter your username',
      password: 'Enter your password',
      nickname: 'Defaults to the username if omitted'
    },
    switcher: {
      login: 'Login',
      register: 'Register'
    },
    footnote: {
      loginPrompt: 'Need an account?',
      loginAction: 'Register here',
      registerPrompt: 'Already have an account?',
      registerAction: 'Back to login'
    },
    validation: {
      missingCredentials: 'Enter both username and password.',
      shortPassword: 'Registration passwords must be at least 4 characters.'
    },
    logout: {
      button: 'Sign out',
      confirm: 'Are you sure you want to sign out?',
      success: 'Signed out',
      error: 'Sign-out request failed, but session was cleared locally'
    }
  },
  flights: {
    console: {
      eyebrow: 'Phase 1',
      title: 'Flight Query System',
      subtitle: 'Flight data query and AI travel advice platform powered by AeroDataBox.',
      badges: {
        dataSource: 'Data Source: AeroDataBox',
        mode: 'Mode: Local MySQL Query'
      }
    },
    sync: {
      eyebrow: 'Flight Sync',
      title: 'Sync flight data',
      description: 'Trigger backend sync, let the crawler write MySQL, then query local data only.',
      form: {
        airportCode: 'Airport Code',
        date: 'Sync Date'
      },
      placeholders: {
        airportCode: 'CKG',
        date: 'Choose sync date'
      },
      actions: {
        syncDate: 'Sync Selected Date',
        syncToday: 'Sync Today'
      },
      messages: {
        success: 'Flight sync completed.',
        failed: 'Flight sync failed.'
      }
    },
    syncResult: {
      eyebrow: 'Sync Result',
      title: 'Latest sync status',
      empty: {
        title: 'No sync result yet',
        description: 'Run a sync first to inspect backend job output.',
        summary: 'Waiting for the first backend sync run.'
      },
      summary: {
        success: 'Backend sync completed and local query results have been refreshed.',
        failed: 'Backend sync failed. Review the returned error message below.',
        empty: 'Sync completed but no flight rows were returned.'
      },
      fields: {
        successCount: 'Success Count',
        failedCount: 'Failed Count',
        source: 'Source',
        requestParams: 'Request Params',
        startedAt: 'Started At',
        finishedAt: 'Finished At',
        errorMessage: 'Error Message'
      }
    },
    search: {
      eyebrow: 'Flight Search',
      title: 'Query local flight snapshots',
      description: 'Keep the existing search flow and query local MySQL flight data only.'
    },
    eyebrow: 'User Flight Search',
    title: 'Search current flight snapshots',
    subtitle: 'This phase-1 page covers search, detail inspection, and price history without pulling admin or AI concerns into the user flow.',
    filters: {
      from: 'From',
      to: 'To',
      date: 'Date',
      source: 'Source'
    },
    advancedFilters: {
      airline: 'Airline',
      priceRange: 'Price Range',
      status: 'Status',
      departSlot: 'Departure Window'
    },
    filterOptions: {
      any: 'Any',
      sourceAny: 'Any source',
      priceAny: 'Any price',
      statusAny: 'Any status',
      slotAny: 'Any time',
      priceLow: '0 - 1000',
      priceMid: '1000 - 2000',
      priceHigh: '2000+',
      statusScheduled: 'Scheduled',
      statusDelayed: 'Delayed',
      statusCancelled: 'Cancelled',
      slotOvernight: 'Overnight',
      slotMorning: 'Morning',
      slotAfternoon: 'Afternoon',
      slotEvening: 'Evening'
    },
    placeholders: {
      from: 'Choose departure airport',
      to: 'Choose arrival airport',
      date: 'Choose departure date',
      source: 'Choose source'
    },
    table: {
      title: 'Matching Flights',
      empty: 'No flights found for the current filters.',
      unknownAirline: 'Unknown airline',
      results: '{count} result in the current query. | {count} results in the current query.',
      columns: {
        flight: 'Flight',
        route: 'Route',
        departure: 'Departure',
        arrival: 'Arrival',
        price: 'Price',
        seats: 'Seats',
        source: 'Source',
        status: 'Status'
      },
      unknownStatus: 'Unknown'
    },
    detail: {
      title: 'Selected Flight Detail',
      eyebrow: 'Selected Flight',
      airline: 'Airline',
      route: 'Route',
      airports: 'Airports',
      departure: 'Departure',
      arrival: 'Arrival',
      seatsLeft: 'Seats Left',
      source: 'Source',
      collected: 'Collected'
    },
    history: {
      title: 'Price History',
      loading: 'Loading price history...',
      empty: 'No price history available for this flight yet.',
      waiting: 'History becomes available after repeated crawls capture the same flight.',
      unknownTime: 'Unknown time',
      summary: '{count} capture recorded, from ¥{lowest} to ¥{highest}. | {count} captures recorded, from ¥{lowest} to ¥{highest}.'
    },
    errors: {
      loadFailed: 'Unable to load flights. Please try again.',
      partialHistory: 'Flight detail loaded partially. Price history is unavailable right now.'
    },
    results: {
      eyebrow: 'Flight Results',
      title: 'Flight query results',
      description: 'The table below is served from the local database through Spring Boot APIs.',
      metrics: {
        total: '{count} results',
        perPage: 'Per page',
        currentPage: 'Page {page}'
      },
      emptyHint: 'No local rows yet. Run a sync first, then search again.',
      emptyTitle: 'No local flights found',
      emptyDescription: 'Try syncing the selected airport and date first, then query the local database again.'
    },
    favorite: {
      added: 'Favorited',
      removed: 'Removed from favorites',
      failed: 'Operation failed, please retry'
    },
    ai: {
      candidateMeta: '¥{price} / {seats} seats / {source}'
    }
  },
  favorites: {
    eyebrow: 'User Profile',
    title: 'Favorites',
    subtitle: 'Review the flights you have already pinned from search results.',
    empty: 'No saved flights yet.',
    errors: {
      loadFailed: 'Unable to load favorites right now.',
      removeFailed: 'Unable to remove this favorite right now.'
    }
  },
  history: {
    eyebrow: 'User Profile',
    title: 'Search History',
    subtitle: 'Recent flight searches are recorded automatically from the current query flow.',
    empty: 'No search history yet.',
    errors: {
      loadFailed: 'Unable to load search history right now.'
    }
  },
  admin: {
    crawlJobs: {
      eyebrow: 'Admin Crawl Jobs',
      title: 'Run crawl jobs through the admin boundary',
      subtitle: 'This page keeps phase-1 crawl administration limited to job creation and recent job inspection.',
      form: {
        source: 'Source',
        from: 'From',
        to: 'To',
        date: 'Date',
        adults: 'Adults',
        maxResults: 'Max Results'
      },
      placeholders: {
        from: 'Shanghai',
        to: 'Beijing'
      },
      actions: {
        submit: 'Create Crawl Job',
        submitting: 'Submitting...',
        refresh: 'Refresh',
        refreshing: 'Refreshing...'
      },
      recentJobs: {
        title: 'Recent Jobs',
        empty: 'No crawl jobs yet.',
        columns: {
          id: 'ID',
          source: 'Source',
          status: 'Status',
          success: 'Success',
          failed: 'Failed',
          started: 'Started'
        }
      },
      errors: {
        noConfiguredSource: 'No configured remote data source is available.',
        loadJobsFailed: 'Unable to load crawl jobs right now.',
        loadStatusesFailed: 'Unable to load data-source status right now.',
        createFailed: 'Unable to create the crawl job.'
      }
    },
    dataSources: {
      eyebrow: 'Admin Data Sources',
      title: 'Data source status',
      subtitle: 'This view reports whether the real remote data source is actually configured. No fallback source is exposed.',
      actions: {
        refresh: 'Refresh',
        refreshing: 'Refreshing...'
      },
      badges: {
        configured: 'Configured',
        notConfigured: 'Not Configured'
      },
      errors: {
        loadFailed: 'Unable to load data-source status right now.'
      }
    }
  }
}
