package fr.haan.bipak.sample.android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import fr.haan.bipak.sample.android.presentation.recyclerview.ItemListFragment
import fr.haan.bipak.sample.android.recyclerview.R
import fr.haan.bipak.sample.android.recyclerview.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fragment = ItemListFragment()
        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, fragment)
            .commit()
    }
}
