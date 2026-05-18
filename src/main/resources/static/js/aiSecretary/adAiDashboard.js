document.addEventListener('DOMContentLoaded', function () {
  const dashboardData = window.adAiDashboardData || {};
  const trendList = Array.isArray(dashboardData.usageTrendList) ? dashboardData.usageTrendList : [];
  const featureList = Array.isArray(dashboardData.featureUsageList) ? dashboardData.featureUsageList : [];
  const selectedPeriod = Number(dashboardData.selectedPeriod || dashboardData.period || 7);

  const chartElement = document.getElementById('aiUsageTrendChart');
  const chartEmptyElement = document.getElementById('aiUsageTrendEmptyState');
  const featureChartElement = document.getElementById('featureUsageChart');
  const featureEmptyElement = document.getElementById('featureUsageEmptyState');
  const csvButton = document.getElementById('downloadAiLogCsvBtn');
  const dateFilterForm = document.getElementById('dashboardDateFilterForm');
  const dateRangeInput = document.getElementById('dashboardDateRangePicker');
  const detailModalElement = document.getElementById('aiLogDetailModal');
  const detailButtons = document.querySelectorAll('.ai-log-detail-btn');
  const startDateField = dateFilterForm ? dateFilterForm.querySelector('input[name="startDate"]') : null;
  const endDateField = dateFilterForm ? dateFilterForm.querySelector('input[name="endDate"]') : null;
  let dateRangePicker = null;

  const fallbackBoxElement = detailModalElement ? detailModalElement.querySelector('[data-log-fallback-box]') : null;
  const fallbackMessageElement = detailModalElement ? detailModalElement.querySelector('[data-log-fallback-message]') : null;
  const fallbackErrorElement = detailModalElement ? detailModalElement.querySelector('[data-log-fallback-error]') : null;

  const featureTooltipMap = {
    '챗봇': 'AI_LOG.type = CHATBOT',
    'AI 비서': 'AI_LOG.type = ASSISTANT',
    '문장 다듬기': 'CORRECTION 메타 기준',
    '템플릿 생성': 'AI_TEMPLATE 생성 기준',
  };

  const normalizeText = function (value) {
    const text = value == null ? '' : String(value).trim();
    return text || '-';
  };

  const padDatePart = function (value) {
    return String(value).padStart(2, '0');
  };

  const toIsoDate = function (date) {
    if (!(date instanceof Date) || Number.isNaN(date.getTime())) {
      return '';
    }

    return [
      date.getFullYear(),
      padDatePart(date.getMonth() + 1),
      padDatePart(date.getDate()),
    ].join('-');
  };

  const getRelativeRange = function (days) {
    const rangeDays = days === 30 ? 30 : 7;
    const endDate = new Date();
    const startDate = new Date(endDate);
    startDate.setDate(endDate.getDate() - (rangeDays - 1));
    return {
      startDate: toIsoDate(startDate),
      endDate: toIsoDate(endDate),
    };
  };

  const getInitialRange = function () {
    const startDate = normalizeText(dashboardData.selectedStartDate || dashboardData.startDate);
    const endDate = normalizeText(dashboardData.selectedEndDate || dashboardData.endDate);
    if (startDate !== '-' && endDate !== '-') {
      return [startDate, endDate];
    }

    const presetRange = getRelativeRange(selectedPeriod);
    return [presetRange.startDate, presetRange.endDate];
  };

  const syncRangeFields = function (selectedDates) {
    if (!startDateField || !endDateField) {
      return;
    }

    if (!Array.isArray(selectedDates) || selectedDates.length === 0) {
      startDateField.value = '';
      endDateField.value = '';
      return;
    }

    const firstDate = toIsoDate(selectedDates[0]);
    const secondDate = selectedDates.length >= 2 ? toIsoDate(selectedDates[1]) : '';

    if (selectedDates.length >= 2 && firstDate && secondDate) {
      startDateField.value = firstDate;
      endDateField.value = secondDate;
      return;
    }

    startDateField.value = firstDate;
    endDateField.value = firstDate;
  };

  const initializeDateRangePicker = function () {
    if (!dateRangeInput || typeof flatpickr !== 'function') {
      return;
    }

    const initialRange = getInitialRange();
    const hasExplicitRange = normalizeText(dashboardData.selectedStartDate || dashboardData.startDate) !== '-'
      && normalizeText(dashboardData.selectedEndDate || dashboardData.endDate) !== '-';
    const config = {
      mode: 'range',
      dateFormat: 'Y-m-d',
      allowInput: false,
      maxDate: 'today',
      locale: typeof flatpickr.l10ns !== 'undefined' && flatpickr.l10ns.ko ? flatpickr.l10ns.ko : undefined,
      defaultDate: initialRange,
      onReady: function (selectedDates, dateStr, instance) {
        if (initialRange[0] && initialRange[1]) {
          instance.input.value = initialRange[0] + ' ~ ' + initialRange[1];
        }
        if (hasExplicitRange) {
          syncRangeFields(selectedDates);
        }
      },
      onChange: function (selectedDates, dateStr, instance) {
        syncRangeFields(selectedDates);
        if (selectedDates.length >= 2) {
          instance.input.value = toIsoDate(selectedDates[0]) + ' ~ ' + toIsoDate(selectedDates[1]);
        }
      },
    };

    dateRangePicker = flatpickr(dateRangeInput, config);
  };

  const escapeHtml = function (value) {
    return normalizeText(value)
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#39;');
  };

  const toggleTrendEmptyState = function (showEmpty) {
    if (chartElement) {
      chartElement.classList.toggle('d-none', showEmpty);
    }
    if (chartEmptyElement) {
      chartEmptyElement.classList.toggle('d-none', !showEmpty);
    }
  };

  const toggleFeatureEmptyState = function (showEmpty) {
    if (featureChartElement) {
      featureChartElement.classList.toggle('d-none', showEmpty);
    }
    if (featureEmptyElement) {
      featureEmptyElement.classList.toggle('d-none', !showEmpty);
    }
  };

  const renderTrendChart = function () {
    if (!chartElement) {
      return;
    }

    if (!trendList.length || typeof ApexCharts === 'undefined') {
      toggleTrendEmptyState(true);
      return;
    }

    const categories = trendList.map(function (item) {
      return normalizeText(item && item.label);
    });

    const totalSeries = trendList.map(function (item) {
      const value = item && item.totalCount != null ? Number(item.totalCount) : Number(item && item.count);
      return Number.isFinite(value) ? value : 0;
    });

    const chatbotSeries = trendList.map(function (item) {
      const value = item && item.chatbotCount != null ? Number(item.chatbotCount) : 0;
      return Number.isFinite(value) ? value : 0;
    });

    const assistantSeries = trendList.map(function (item) {
      const value = item && item.assistantCount != null ? Number(item.assistantCount) : 0;
      return Number.isFinite(value) ? value : 0;
    });

    const allZero = totalSeries.every(function (value) {
      return value === 0;
    }) && chatbotSeries.every(function (value) {
      return value === 0;
    }) && assistantSeries.every(function (value) {
      return value === 0;
    });

    try {
      const chart = new ApexCharts(chartElement, {
        chart: {
          type: 'area',
          height: 280,
          toolbar: { show: false },
          zoom: { enabled: false },
          foreColor: '#6b7280',
        },
        series: [
          {
            name: '전체 요청 수',
            data: totalSeries,
          },
          {
            name: '챗봇',
            data: chatbotSeries,
          },
          {
            name: 'AI 비서',
            data: assistantSeries,
          },
        ],
        colors: ['#2563eb', '#0ea5e9', '#8b5cf6'],
        dataLabels: { enabled: false },
        stroke: {
          curve: 'smooth',
          width: [3, 2, 2],
        },
        fill: {
          type: 'gradient',
          gradient: {
            shadeIntensity: 1,
            opacityFrom: 0.28,
            opacityTo: 0.05,
            stops: [0, 90, 100],
          },
        },
        grid: {
          borderColor: '#e5e7eb',
          strokeDashArray: 4,
        },
        markers: {
          size: 4,
          hover: {
            size: 6,
          },
        },
        xaxis: {
          categories: categories,
          tickAmount: categories.length,
          labels: {
            rotate: -45,
            rotateAlways: true,
            hideOverlappingLabels: false,
            trim: false,
            style: {
              colors: '#6b7280',
            },
          },
        },
        yaxis: {
          min: 0,
          forceNiceScale: true,
          labels: {
            style: {
              colors: '#6b7280',
            },
          },
        },
        tooltip: {
          theme: 'light',
          shared: true,
          intersect: false,
        },
        legend: {
          show: true,
          position: 'top',
          horizontalAlign: 'left',
        },
        noData: {
          text: '데이터가 없습니다.',
        },
      });

      chart.render();
      toggleTrendEmptyState(false);

      if (allZero) {
        chart.updateOptions({
          yaxis: {
            min: 0,
            max: 1,
            forceNiceScale: false,
          },
        }, false, true);
      }
    } catch (error) {
      toggleTrendEmptyState(true);
    }
  };

  const renderFeatureChart = function () {
    if (!featureChartElement) {
      return;
    }

    const totalCount = featureList.reduce(function (sum, item) {
      const value = item && item.count != null ? Number(item.count) : 0;
      return sum + (Number.isFinite(value) ? value : 0);
    }, 0);

    if (!featureList.length || totalCount <= 0 || typeof ApexCharts === 'undefined') {
      toggleFeatureEmptyState(true);
      return;
    }

    const series = featureList.map(function (item) {
      const value = item && item.count != null ? Number(item.count) : 0;
      return Number.isFinite(value) ? value : 0;
    });

    const labels = featureList.map(function (item) {
      return normalizeText(item && item.label);
    });

    const tooltipData = featureList.map(function (item) {
      const label = normalizeText(item && item.label);
      const count = item && item.count != null ? Number(item.count) : 0;
      const percent = item && item.percentage != null ? Number(item.percentage) : 0;
      return {
        label: label,
        count: Number.isFinite(count) ? count : 0,
        percent: Number.isFinite(percent) ? percent : 0,
        description: featureTooltipMap[label] || '-',
      };
    });

    try {
      const chart = new ApexCharts(featureChartElement, {
        chart: {
          type: 'donut',
          height: 300,
          toolbar: { show: false },
        },
        series: series,
        labels: labels,
        colors: ['#2563eb', '#0ea5e9', '#f59e0b', '#8b5cf6'],
        stroke: {
          width: 2,
          colors: ['#ffffff'],
        },
        dataLabels: {
          enabled: false,
        },
        legend: {
          show: true,
          position: 'bottom',
          horizontalAlign: 'center',
        },
        plotOptions: {
          pie: {
            donut: {
              size: '68%',
              labels: {
                show: true,
                total: {
                  show: true,
                  label: '총 사용량',
                  formatter: function () {
                    return String(totalCount);
                  },
                },
              },
            },
          },
        },
        tooltip: {
          theme: 'light',
          custom: function ({ series, seriesIndex }) {
            const item = tooltipData[seriesIndex] || {};
            const count = Number.isFinite(series[seriesIndex]) ? series[seriesIndex] : 0;
            const percent = totalCount > 0 ? Math.round((count * 1000) / totalCount) / 10 : 0;

            return [
              '<div class="p-3">',
              '<div class="fw-bold mb-1">' + escapeHtml(item.label) + '</div>',
              '<div class="small text-muted mb-1">건수: ' + count + '건</div>',
              '<div class="small text-muted mb-1">비율: ' + percent + '%</div>',
              '<div class="small text-muted">집계 기준: ' + escapeHtml(item.description) + '</div>',
              '</div>'
            ].join('');
          },
        },
        noData: {
          text: '데이터가 없습니다.',
        },
      });

      chart.render();
      toggleFeatureEmptyState(false);
    } catch (error) {
      toggleFeatureEmptyState(true);
    }
  };

  const bindCsvDownload = function () {
    if (!csvButton) {
      return;
    }

    csvButton.addEventListener('click', function () {
      const params = new URLSearchParams(window.location.search);
      const exportParams = new URLSearchParams();

      exportParams.set('period', params.get('period') || String(dashboardData.period || dashboardData.selectedPeriod || 7));

      ['startDate', 'endDate', 'department', 'aiType', 'result'].forEach(function (key) {
        const value = params.get(key);
        if (value != null && value !== '') {
          exportParams.set(key, value);
        }
      });

      window.location.href = '/admin/AiSecretary/dashboard/download/csv?' + exportParams.toString();
    });
  };

  const fillModalField = function (fieldName, value) {
    if (!detailModalElement) {
      return;
    }

    const target = detailModalElement.querySelector('[data-log-field="' + fieldName + '"]');
    if (target) {
      target.textContent = normalizeText(value);
    }
  };

  const fillModalSummary = function (fieldName, value) {
    if (!detailModalElement) {
      return;
    }

    const target = detailModalElement.querySelector('[data-log-summary-field="' + fieldName + '"]');
    if (target) {
      target.textContent = normalizeText(value);
    }
  };

  const fillModalMessageContent = function (value) {
    if (!detailModalElement) {
      return;
    }

    const target = detailModalElement.querySelector('[data-log-summary-field="messageContent"]');
    if (target) {
      const normalized = normalizeText(value);
      target.textContent = normalized === '-' ? '연결된 메시지가 없습니다.' : normalized;
    }
  };

  const setFallbackBox = function (button) {
    if (!fallbackBoxElement) {
      return;
    }

    const isFallback = String(button && button.dataset && button.dataset.fallback || '').toLowerCase() === 'true';
    const errorMessage = normalizeText(button && button.dataset ? button.dataset.errorMessage : null);

    if (!isFallback) {
      fallbackBoxElement.classList.add('d-none');
      if (fallbackMessageElement) {
        fallbackMessageElement.textContent = 'AI 응답 생성 중 대체 응답이 사용되었습니다.';
      }
      if (fallbackErrorElement) {
        fallbackErrorElement.textContent = '';
      }
      return;
    }

    fallbackBoxElement.classList.remove('d-none');
    if (fallbackMessageElement) {
      fallbackMessageElement.textContent = 'AI 응답 생성 중 대체 응답이 사용되었습니다.';
    }
    if (fallbackErrorElement) {
      fallbackErrorElement.textContent = errorMessage === '-' ? '' : '오류 메시지: ' + errorMessage;
    }
  };

  const openDetailModal = function (button) {
    if (!detailModalElement || !button) {
      return;
    }

    fillModalField('user', button.dataset.user);
    fillModalField('department', button.dataset.department);
    fillModalField('type', button.dataset.type);
    fillModalField('resultLabel', button.dataset.resultLabel);
    fillModalField('durationText', button.dataset.durationText);
    fillModalField('createdAt', button.dataset.createdAt);
    fillModalField('sessionId', button.dataset.sessionId);
    fillModalField('messageId', button.dataset.messageId);

    fillModalSummary('requestSummary', button.dataset.requestSummary);
    fillModalSummary('responseSummary', button.dataset.responseSummary);
    fillModalMessageContent(button.dataset.messageContent);
    setFallbackBox(button);

    if (window.bootstrap && window.bootstrap.Modal) {
      window.bootstrap.Modal.getOrCreateInstance(detailModalElement).show();
      return;
    }

    detailModalElement.classList.add('show');
    detailModalElement.style.display = 'block';
    detailModalElement.setAttribute('aria-hidden', 'false');
    document.body.classList.add('modal-open');
  };

  const bindDetailButtons = function () {
    if (!detailButtons || !detailButtons.length) {
      return;
    }

    detailButtons.forEach(function (button) {
      button.addEventListener('click', function () {
        openDetailModal(button);
      });
    });
  };

  if (dateFilterForm) {
    dateFilterForm.addEventListener('submit', function () {
      if (dateRangePicker && Array.isArray(dateRangePicker.selectedDates)) {
        syncRangeFields(dateRangePicker.selectedDates);
      }
    });
  }

  initializeDateRangePicker();
  renderTrendChart();
  renderFeatureChart();
  bindCsvDownload();
  bindDetailButtons();
});
