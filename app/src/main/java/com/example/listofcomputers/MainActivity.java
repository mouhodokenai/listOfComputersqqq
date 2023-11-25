package com.example.listofcomputers;

import static com.example.listofcomputers.MainActivity.adapter;
import static com.example.listofcomputers.MainActivity.dataItems;
import static com.example.listofcomputers.MainActivity.serverAccessor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.Manifest;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static ListView listView;
    static ArrayAdapter adapter;
    static ArrayList<Computer> dataItems;
    private Fragment_info infoFragment;
    private Fragment_add addFragment;
    private static final int MY_PERMISSIONS_REQUEST_INTERNET = 123; // или любое другое уникальное число
    private static final String SERVICE_ADDRESS = "http://37.77.105.18/api/Desktops";
    public static ServerAccessor serverAccessor = new ServerAccessor(SERVICE_ADDRESS);
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.listView);
        dataItems = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, dataItems);
        listView.setAdapter(adapter);

        // Инициализация фрагментов
        infoFragment = new Fragment_info();
        addFragment = new Fragment_add();

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            // Разрешение не предоставлено, запросить его у пользователя
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, MY_PERMISSIONS_REQUEST_INTERNET);
        }

        //Запуск фоновой задачи
        ProgressTask progressTask = new ProgressTask();
        executorService.submit(progressTask);

        FragmentManager fragmentManager = getSupportFragmentManager();
/*
        // Инициализация данных из базы данных
        DataBaseAccessor databaseAccessor = new DataBaseAccessor(this);
        dataItems.addAll(databaseAccessor.getAllData());
 */
        adapter.notifyDataSetChanged();

        // Обработчик кнопки "Добавить"
        Button addButton = findViewById(R.id.addButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(R.id.fragment_container, addFragment);
                transaction.commit();
            }
        });

        // Обработчик нажатия на элемент списка
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Bundle bundle = new Bundle();
            bundle.putSerializable("computer", dataItems.get(position)); // Передаем выбранный объект Computer

            int idComp = dataItems.get(position).getId();
            String nameComp = dataItems.get(position).getName();
            String statusComp = dataItems.get(position).getStatus();
            String locationComp = dataItems.get(position).getLocation();
            String onlineComp = dataItems.get(position).getLastOnline();
            bundle.putInt("id", idComp);
            bundle.putString("name", nameComp);
            bundle.putString("status", statusComp);
            bundle.putString("location", locationComp);
            bundle.putString("online", onlineComp);
            bundle.putInt("number", position);                           // Передаем номер элемента в списке

            infoFragment.setArguments(bundle);

            // Создаем объект FragmentTransaction для управления транзакциями фрагментов
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            // Заменяем текущий фрагмент в контейнере с идентификатором R.id.fragment_container на новый фрагмент (infoFragment)
            transaction.replace(R.id.fragment_container, infoFragment);
            // Применение изменений
            transaction.commit();
        });
    }
    public ArrayAdapter<String> AdapterUpdate(ArrayList<Computer> list) {
        ArrayList<String> stringList = serverAccessor.getStringListFromNoteList(list);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, dataItems);
        // установить адаптер в listview
        MainActivity.listView.setAdapter(adapter);
        return adapter;
    }

    // Метод для определения, является ли устройство планшетом
    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }
}
class ProgressTask implements Runnable {
    String connectionError = null;

    @Override
    public void run() {
        try {
            // выполнение в фоне
            dataItems = serverAccessor.getData();

            // Обновление UI осуществляется в основном потоке
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (connectionError == null) {
                        adapter = AdapterUpdate(dataItems);
                    } else {
                        //проблемы с интернетом
                    }
                }
            });

        } catch (Exception ex) {
            connectionError = ex.getMessage();
        }
    }
}
