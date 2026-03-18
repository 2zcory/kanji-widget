package com.example.kanjiwidget.stats

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import com.example.kanjiwidget.KanjiDetailNavigator
import com.example.kanjiwidget.R
import com.example.kanjiwidget.theme.ThemeController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class StudyStatsDialogFragment : BottomSheetDialogFragment() {

    private val viewModel: StudyStatsViewModel by activityViewModels()
    
    private lateinit var chartView: StudyTimeChartView
    private lateinit var progressToday: ProgressBar
    private lateinit var tvStreak: TextView
    private lateinit var tvStreakLabel: TextView
    private lateinit var containerInsights: LinearLayout
    private lateinit var containerRanking: LinearLayout
    private lateinit var containerRankingList: LinearLayout
    private lateinit var btnViewAll: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.view_actionable_insight_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(view)
        observeViewModel()
        
        viewModel.refreshData()
    }

    private fun setupViews(view: View) {
        chartView = view.findViewById(R.id.dashboardChartView)
        progressToday = view.findViewById(R.id.progressToday)
        tvStreak = view.findViewById(R.id.tvDashboardStreak)
        tvStreakLabel = view.findViewById(R.id.tvDashboardStreakLabel)
        containerInsights = view.findViewById(R.id.containerDashboardInsights)
        containerRanking = view.findViewById(R.id.containerDashboardRanking)
        containerRankingList = view.findViewById(R.id.containerRankingList)
        btnViewAll = view.findViewById(R.id.btnViewAllRanking)

        btnViewAll.setOnClickListener {
            expandToFull()
        }

        applyTheme(view)
    }

    private fun observeViewModel() {
        viewModel.statsSummary.observe(viewLifecycleOwner) { summary ->
            bindStats(summary)
        }
        
        viewModel.difficultKanji.observe(viewLifecycleOwner) { items ->
            bindInsights(items)
        }

        viewModel.ranking.observe(viewLifecycleOwner) { ranking ->
            bindRanking(ranking)
        }
    }

    private fun bindStats(summary: StudyChartSummary) {
        chartView.points = summary.points
        
        val streak = summary.currentStreakDays
        tvStreak.text = getString(R.string.widget_streak_value, streak)
        tvStreakLabel.text = if (streak > 0) {
            getString(R.string.dashboard_streak_active)
        } else {
            getString(R.string.dashboard_streak_inactive)
        }

        val goalMs = 30 * 60 * 1000L // 30 mins goal
        val todayMs = summary.points.lastOrNull()?.totalMs ?: 0L
        val progress = ((todayMs.toFloat() / goalMs) * 100).toInt().coerceIn(0, 100)
        progressToday.progress = progress
    }

    private fun bindInsights(items: List<KanjiStudyRankItem>) {
        containerInsights.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        
        if (items.isEmpty()) {
            view?.findViewById<View>(R.id.cardInsights)?.visibility = View.GONE
            return
        }
        
        view?.findViewById<View>(R.id.cardInsights)?.visibility = View.VISIBLE
        items.forEach { item ->
            val row = inflater.inflate(R.layout.item_dashboard_insight, containerInsights, false)
            row.findViewById<TextView>(R.id.tvInsightKanji).text = item.kanji
            row.findViewById<TextView>(R.id.tvInsightName).text = item.meaning ?: item.kanji
            
            val durationText = formatDurationForUi(item.totalStudyMs)
            row.findViewById<TextView>(R.id.tvInsightReason).text = 
                getString(R.string.dashboard_insight_difficult_reason, item.openCount, durationText)
            
            row.findViewById<Button>(R.id.btnInsightReview).setOnClickListener {
                startActivity(buildDetailIntent(item))
            }
            
            row.setOnClickListener { startActivity(buildDetailIntent(item)) }
            containerInsights.addView(row)
        }
    }

    private fun bindRanking(ranking: KanjiStudyRanking) {
        containerRankingList.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        
        ranking.mostRanked.forEach { item ->
            val row = inflater.inflate(R.layout.item_kanji_ranking, containerRankingList, false)
            row.findViewById<TextView>(R.id.tvRankingKanji).text = item.kanji
            row.findViewById<TextView>(R.id.tvRankingPrimary).text = item.meaning ?: item.kanji
            
            val durationText = formatDurationForUi(item.totalStudyMs)
            row.findViewById<TextView>(R.id.tvRankingDuration).text = durationText
            
            row.setOnClickListener { startActivity(buildDetailIntent(item)) }
            containerRankingList.addView(row)
        }
    }

    private fun buildDetailIntent(item: KanjiStudyRankItem): Intent {
        return KanjiDetailNavigator.buildDetailIntent(
            context = requireContext(),
            kanji = item.kanji,
            meaningFallback = item.meaning,
            jlptFallback = item.jlptLevel,
        )
    }

    private fun formatDurationForUi(durationMs: Long): String {
        val totalSeconds = durationMs / 1000L
        val minutes = totalSeconds / 60L
        return if (minutes > 0) "${minutes}m" else "${totalSeconds}s"
    }

    private fun expandToFull() {
        val behavior = BottomSheetBehavior.from(view?.parent as View)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        containerRanking.visibility = View.VISIBLE
        btnViewAll.visibility = View.GONE
    }

    private fun applyTheme(view: View) {
        ThemeController.applyGlassDepth(view.findViewById(R.id.statsDashboardRoot), elevatedDp = 0f)
        ThemeController.applyGlassDepth(view.findViewById(R.id.cardMomentum), elevatedDp = 4f)
        ThemeController.applyGlassDepth(view.findViewById(R.id.cardRhythm), elevatedDp = 4f)
        ThemeController.applyGlassDepth(view.findViewById(R.id.cardInsights), elevatedDp = 4f)
    }

    companion object {
        const val TAG = "StudyStatsDialogFragment"
        fun newInstance() = StudyStatsDialogFragment()
    }
}
