package com.covidtracker.vitemadose.home

import com.covidtracker.vitemadose.data.Department
import com.covidtracker.vitemadose.data.DisplayItem

interface MainContract {

    interface View {

        /**
         * Display main list of centers (available/unavailable)
         */
        fun showCenters(list: List<DisplayItem>)

        /**
         * Setup department selectors with retrieved departments
         */
        fun setupSelector(items: List<Department>, indexSelected: Int)

        /**
         * Open link
         */
        fun openLink(url: String)

        fun showCentersError()
        fun setLoading(loading: Boolean)
    }

    interface Presenter {

        /**
         * Load all departments available
         */
        fun loadDepartments()

        /**
         * Load centers for the saved department
         */
        fun loadCenters()

        /**
         * Called when a center is clicked
         */
        fun onCenterClicked(center: DisplayItem.Center)

        /**
         * Called when a department is selected via the selector
         */
        fun onDepartmentSelected(department: Department)
    }
}