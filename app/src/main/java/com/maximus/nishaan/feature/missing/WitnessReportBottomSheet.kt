package com.maximus.nishaan.feature.missing

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.maximus.nishaan.databinding.DialogWitnessReportBinding
import com.maximus.nishaan.domain.model.WitnessReport
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Slide-up Bottom Sheet for reporting witness sightings (Feature 5).
 */
class WitnessReportBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogWitnessReportBinding? = null
    private val binding get() = _binding!!

    private var selectedSightingTime: Calendar = Calendar.getInstance()
    private var isTimeSelected = false

    private var onSubmitListener: ((WitnessReport) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogWitnessReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup Sighting Time Selector
        binding.btnSelectTime.setOnClickListener {
            showDateTimePicker()
        }

        // Setup Neighborhood dropdown
        val neighborhoods = listOf("Gulshan", "Saddar", "Korangi", "Lyari", "DHA", "Clifton", "Orangi", "Malir", "Kemari", "Nazimabad")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, neighborhoods)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerNeighborhood.adapter = adapter

        // Setup Anonymous Checkbox
        binding.cbAnonymous.setOnCheckedChangeListener { _, isChecked ->
            binding.inputLayoutContact.isEnabled = !isChecked
            if (isChecked) {
                binding.etContact.text = null
            }
        }

        // Setup Submit Button
        binding.btnSubmitReport.setOnClickListener {
            validateAndSubmit()
        }
    }

    private fun showDateTimePicker() {
        val current = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                selectedSightingTime.set(Calendar.YEAR, year)
                selectedSightingTime.set(Calendar.MONTH, month)
                selectedSightingTime.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                TimePickerDialog(
                    requireContext(),
                    { _, hourOfDay, minute ->
                        selectedSightingTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        selectedSightingTime.set(Calendar.MINUTE, minute)
                        isTimeSelected = true

                        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                        binding.btnSelectTime.text = sdf.format(selectedSightingTime.time)
                    },
                    current.get(Calendar.HOUR_OF_DAY),
                    current.get(Calendar.MINUTE),
                    true
                ).show()
            },
            current.get(Calendar.YEAR),
            current.get(Calendar.MONTH),
            current.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun validateAndSubmit() {
        val visualDetails = binding.etVisualDetails.text?.toString()?.trim().orEmpty()
        if (visualDetails.length < 10) {
            binding.inputLayoutVisualDetails.error = "Visual details must be at least 10 characters / کم از کم 10 حروف ہونا ضروری ہے"
            return
        } else {
            binding.inputLayoutVisualDetails.error = null
        }

        if (!isTimeSelected) {
            Toast.makeText(requireContext(), "Please select sighting time / وقت کا انتخاب کریں", Toast.LENGTH_SHORT).show()
            return
        }

        val anonymous = binding.cbAnonymous.isChecked
        val contactInfo = if (anonymous) null else binding.etContact.text?.toString()?.trim()

        val neighborhood = binding.spinnerNeighborhood.selectedItem.toString()

        val report = WitnessReport(
            sightingTime = selectedSightingTime.timeInMillis,
            neighborhood = neighborhood,
            visualDetails = visualDetails,
            contactInfo = contactInfo,
            anonymous = anonymous,
            timestamp = System.currentTimeMillis()
        )

        onSubmitListener?.invoke(report)
        dismiss()
    }

    fun setOnSubmitListener(listener: (WitnessReport) -> Unit) {
        onSubmitListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "WitnessReportBottomSheet"
        fun newInstance() = WitnessReportBottomSheet()
    }
}
