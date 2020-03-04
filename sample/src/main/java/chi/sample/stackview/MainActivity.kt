package chi.sample.stackview

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import chi.widget.OnChangeListener
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val data = listOf(
            "${getString(R.string.dummy_text)} 1",
            "${getString(R.string.dummy_text)} 2",
            "${getString(R.string.dummy_text)} 3",
            "${getString(R.string.dummy_text)} 4",
            "${getString(R.string.dummy_text)} 5",
            "${getString(R.string.dummy_text)} 6",
            "${getString(R.string.dummy_text)} 7",
            "${getString(R.string.dummy_text)} 8",
            "${getString(R.string.dummy_text)} 9",
            "${getString(R.string.dummy_text)} 10",
            "${getString(R.string.dummy_text)} 11",
            "${getString(R.string.dummy_text)} 12",
            "${getString(R.string.dummy_text)} 13",
            "${getString(R.string.dummy_text)} 14",
            "${getString(R.string.dummy_text)} 15",
            "${getString(R.string.dummy_text)} 16",
            "${getString(R.string.dummy_text)} 17",
            "${getString(R.string.dummy_text)} 18",
            "${getString(R.string.dummy_text)} 19",
            "${getString(R.string.dummy_text)} 20"
        )
        stackView.adapter = StackedCardsAdapter(applicationContext, data)
        stackView.onChangeListener = object : OnChangeListener {
            override fun onChange(remainingCardsCount: Int, totalCardsCount: Int) {
                Log.d("StackView", "remainingCardsCount :: $remainingCardsCount, totalCardsCount :: $totalCardsCount")
            }
        }
    }

    class StackedCardsAdapter(
        private val context: Context,
        private val data: List<String>
    ) : BaseAdapter() {

        override fun getCount(): Int {
            return data.size
        }

        override fun getItem(position: Int): String {
            return data[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(
            position: Int,
            convertView: View?,
            parent: ViewGroup?
        ): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.card, parent, false)
            view.also {
                it.findViewById<TextView>(R.id.textView).text = data[position]
            }

            return view
        }

    }

}
