package com.example.kanjiwidget.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.kanjiwidget.home.HomeSummary
import com.example.kanjiwidget.home.HomeSummaryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudyStatsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StudyStatsRepository(application)
    private val homeRepository = HomeSummaryRepository(application)
    private val rankingRepository = KanjiRankingRepository(application)

    private val _statsSummary = MutableLiveData<StudyChartSummary>()
    val statsSummary: LiveData<StudyChartSummary> = _statsSummary

    private val _homeSummary = MutableLiveData<HomeSummary>()
    val homeSummary: LiveData<HomeSummary> = _homeSummary

    private val _ranking = MutableLiveData<KanjiStudyRanking>()
    val ranking: LiveData<KanjiStudyRanking> = _ranking

    private val _difficultKanji = MutableLiveData<List<KanjiStudyRankItem>>()
    val difficultKanji: LiveData<List<KanjiStudyRankItem>> = _difficultKanji

    fun refreshData(days: Int = 7, scope: RankingScope = RankingScope.ALL_TIME, metric: RankingMetric = RankingMetric.STUDY_TIME) {
        viewModelScope.launch {
            val stats = withContext(Dispatchers.IO) { repository.getDailyChart(days) }
            val home = withContext(Dispatchers.IO) { homeRepository.loadSummary() }
            val rank = withContext(Dispatchers.IO) { rankingRepository.getRanking(scope, metric) }
            val difficult = withContext(Dispatchers.IO) { rankingRepository.getDifficultKanji() }
            
            _statsSummary.value = stats
            _homeSummary.value = home
            _ranking.value = rank
            _difficultKanji.value = difficult
        }
    }
}
