package com.benforino.trailtrackerv2
import android.os.Bundle
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.firebase.ui.auth.AuthUI

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        val key = preference.key
        if(key == "logout"){
            signOut()
            findNavController().navigate(R.id.action_settingsFragment_to_FirstFragment)
        }
        return super.onPreferenceTreeClick(preference)
    }

    private fun signOut() {
        // [START auth_fui_signout]
        this.context?.let {
            AuthUI.getInstance()
                .signOut(it)
                .addOnCompleteListener {
                    Toast.makeText(this.context, "Signed Out", Toast.LENGTH_SHORT).show();
                }
        }
        // [END auth_fui_signout]
    }

}