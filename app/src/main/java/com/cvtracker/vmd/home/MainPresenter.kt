package com.cvtracker.vmd.home

import com.cvtracker.vmd.data.DisplayItem
import com.cvtracker.vmd.data.SearchEntry
import com.cvtracker.vmd.master.AnalyticsHelper
import com.cvtracker.vmd.master.DataManager
import com.cvtracker.vmd.master.FilterType
import com.cvtracker.vmd.master.PrefHelper
import kotlinx.coroutines.*
import timber.log.Timber

class MainPresenter(private val view: MainContract.View) : MainContract.Presenter {

    private var jobSearch: Job? = null
    private var jobCenters: Job? = null

    private var selectedFilter: FilterType? = null

    companion object{
        const val DISPLAY_CENTER_MAX_DISTANCE_IN_KM = 75f
    }

    override fun loadInitialState() {
        PrefHelper.favEntry.let { entry ->
            if (entry == null) {
                view.showEmptyState()
            }
            view.displaySelectedSearchEntry(entry)
        }
    }

    override fun loadCenters() {
        jobCenters?.cancel()
        jobCenters = GlobalScope.launch(Dispatchers.Main) {
            PrefHelper.favEntry?.let { entry ->
                try {
                    val filter = selectedFilter ?: entry.defaultFilterType
                    val isCitySearch = entry is SearchEntry.City

                    view.setLoading(true)

                    DataManager.getCenters(
                        departmentCode = entry.entryDepartmentCode,
                        useNearDepartment = isCitySearch
                    ).let {
                        val list = mutableListOf<DisplayItem>()

                        fun addCenters(centers: MutableList<DisplayItem.Center>, available: Boolean) {
                            /** Set up distance when city search **/
                            if (isCitySearch) {
                                centers.onEach { it.calculateDistance(entry as SearchEntry.City) }
                                centers.removeAll {
                                    (it.distance ?: 0f) > DISPLAY_CENTER_MAX_DISTANCE_IN_KM
                                }
                            }
                            /** Sort results **/
                            centers.sortWith(filter.comparator)
                            list.addAll(centers.onEach { it.available = available })
                        }

                        /** Add header to show last updated view **/
                        list.add(DisplayItem.LastUpdated(it.lastUpdated))

                        if (it.availableCenters.isNotEmpty()) {
                            /** Add header when available centers **/
                            list.add(
                                DisplayItem.AvailableCenterHeader(
                                    it.availableCenters.size,
                                    it.availableCenters.sumBy { it.appointmentCount })
                            )

                            addCenters(it.availableCenters, true)
                        }

                        if (it.unavailableCenters.isNotEmpty()) {
                            /** Add the header with unavailable centers **/
                            list.add(DisplayItem.UnavailableCenterHeader(it.availableCenters.isNotEmpty()))

                            addCenters(it.unavailableCenters, false)
                        }

                        view.showCenters(list, if (isCitySearch) filter else null)
                        AnalyticsHelper.logEventSearch(entry, it, filter)
                    }
                } catch (e: CancellationException) {
                    /** Coroutine has been canceled => Ignore **/
                } catch (e: Exception){
                    Timber.e(e)
                    view.showCentersError()
                } finally {
                    view.setLoading(false)
                }
            }
        }
    }

    override fun onSearchEntrySelected(searchEntry: SearchEntry) {
        if (searchEntry.entryCode != PrefHelper.favEntry?.entryCode) {
            PrefHelper.favEntry = searchEntry
            view.showCenters(emptyList(), null)
        }
        selectedFilter = null
        loadCenters()
    }

    override fun getSavedSearchEntry(): SearchEntry? {
        return PrefHelper.favEntry
    }

    override fun onCenterClicked(center: DisplayItem.Center) {
        view.openLink(center.url)
        AnalyticsHelper.logEventRdvClick(center, FilterType.ByDate)
    }

    override fun onFilterChanged(filter: FilterType) {
        selectedFilter = filter
        view.showCenters(emptyList(), filter)
        loadCenters()
    }

    override fun onSearchUpdated(search: String) {
        jobSearch?.cancel()
        if (search.length < 2) {
            /** reset entry list **/
            view.setupSelector(emptyList())
            return
        }
        jobSearch = GlobalScope.launch(Dispatchers.Main) {
            /** Wait a bit then we are sure the user want to do this one **/
            delay(250)
            val list = mutableListOf<SearchEntry>()
            try {
                if (search.first().isDigit()) {
                    /** Search by code **/
                    list.addAll(DataManager.getDepartmentsByCode(search))
                    list.addAll(DataManager.getCitiesByPostalCode(search))
                } else {
                    /** Search by name **/
                    list.addAll(DataManager.getDepartmentsByName(search))
                    list.addAll(DataManager.getCitiesByName(search))
                }
                view.setupSelector(list)
            } catch (e: CancellationException) {
                /** Coroutine has been canceled => Ignore **/
            } catch (e: Exception) {
                Timber.e(e)
                view.showSearchError()
            }
        }
    }

}