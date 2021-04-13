package com.cvtracker.vmd.home

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.cvtracker.vmd.R
import com.cvtracker.vmd.about.AboutActivity
import com.cvtracker.vmd.data.Department
import com.cvtracker.vmd.data.DisplayItem
import com.cvtracker.vmd.extensions.*
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.empty_state.*
import kotlinx.android.synthetic.main.empty_state.view.*
import java.net.URLEncoder

class MainActivity : AppCompatActivity(), MainContract.View {

    private val presenter: MainContract.Presenter = MainPresenter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.setBackgroundDrawable(ColorDrawable(colorAttr(R.attr.backgroundColor)))

        presenter.loadDepartments()
        presenter.loadCenters()

        refreshLayout.setOnRefreshListener {
            presenter.loadCenters()
        }

        aboutIconView.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            val progress = (-verticalOffset / headerLayout.measuredHeight.toFloat()) * 1.5f
            headerLayout.alpha = 1 - progress
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                loadColor(colorAttr(R.attr.iconTintColor), color(R.color.white), progress) {
                    aboutIconView.imageTintList = ColorStateList.valueOf(it)
                }
                loadColor(
                    colorAttr(R.attr.backgroundColor),
                    colorAttr(R.attr.colorPrimary),
                    progress
                ) {
                    backgroundSelectorView.setBackgroundColor(it)
                    appBarLayout.setBackgroundColor(it)
                }
                if (isDarkTheme()) {
                    loadColor(
                        colorAttr(android.R.attr.textColorPrimary),
                        color(R.color.mine_shaft),
                        progress
                    ) {
                        selectedDepartment.setTextColor(it)
                    }
                    loadColor(
                        colorAttr(R.attr.backgroundCardColor),
                        color(R.color.grey_5),
                        progress
                    ) {
                        departmentSelector.setCardBackgroundColor(it)
                    }
                }
            }
        })
    }

    override fun showCenters(list: List<DisplayItem>) {
        centersRecyclerView.layoutManager = LinearLayoutManager(this)
        centersRecyclerView.adapter = CenterAdapter(
            context = this,
            items = list,
            onClicked = { presenter.onCenterClicked(it) },
            onAddressClicked = { startMapsActivity(it) },
            onPhoneClicked = { startPhoneActivity(it) }
        )

        emptyStateContainer?.hide()
    }

    private fun startPhoneActivity(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Snackbar.make(
                container,
                getString(R.string.no_app_activity_found),
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun startMapsActivity(address: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("geo:0,0?q=${URLEncoder.encode(address, "utf-8")}")
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Snackbar.make(
                container,
                getString(R.string.no_app_activity_found),
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    override fun setLoading(loading: Boolean) {
        refreshLayout.isRefreshing = loading
    }

    override fun setupSelector(items: List<Department>, indexSelected: Int) {
        val array = items.map { "${it.departmentCode} - ${it.departmentName}" }.toTypedArray()
        arrayOf(emptyStateDepartmentSelector, departmentSelector).filterNotNull()
            .forEach { selector ->
                selector.setOnClickListener {
                    AlertDialog.Builder(this)
                        .setTitle(R.string.choose_department_title)
                        .setItems(array) { dialogInterface, index ->
                            presenter.onDepartmentSelected(items[index])
                            displaySelectedDepartment(items[index])
                            dialogInterface.dismiss()
                        }.create().show()
                }
                displaySelectedDepartment(items.getOrNull(indexSelected))
            }
    }

    override fun showEmptyState() {
        stubEmptyState.setOnInflateListener { stub, inflated ->
            SpannableString(inflated.emptyStateBaselineTextView.text).apply {
                setSpan(
                    ForegroundColorSpan(colorAttr(R.attr.colorPrimary)),
                    27,
                    37,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                setSpan(
                    ForegroundColorSpan(color(R.color.blue_main)),
                    41,
                    51,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                inflated.emptyStateBaselineTextView.setText(this, TextView.BufferType.SPANNABLE)
            }
        }
        stubEmptyState.inflate()
    }

    private fun displaySelectedDepartment(department: Department?) {
        arrayOf(emptyStateSelectedDepartment, selectedDepartment).filterNotNull().forEach {
            it.text = if (department != null) {
                "${department.departmentCode} - ${department.departmentName}"
            } else {
                getString(R.string.choose_department_title)
            }
        }
    }

    override fun openLink(url: String) {
        launchWebUrl(url)
    }

    override fun showCentersError() {
        Snackbar.make(container, getString(R.string.centers_error), Snackbar.LENGTH_SHORT).show()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    fun loadColor(
        colorStart: Int,
        colorEnd: Int,
        progress: Float,
        onColorLoaded: (Int) -> Unit
    ) {
        ValueAnimator.ofObject(ArgbEvaluator(), colorStart, colorEnd).apply {
            setCurrentFraction(progress)
            onColorLoaded.invoke(animatedValue as Int)
        }
    }
}