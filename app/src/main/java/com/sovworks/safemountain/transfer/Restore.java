package com.sovworks.safemountain;

import android.os.AsyncTask;

// < >안에 들은 자료형은 순서대로 doInBackground, onProgressUpdate, onPostExecute의 매개변수 자료형을 뜻한다.(내가 사용할 매개변수타입을 설정하면된다)
public class Restore extends AsyncTask<Void , Integer , Integer> {
    //초기화 단계에서 사용한다. 초기화관련 코드를 작성했다.
    @Override
    protected void onPreExecute() {

    }

    //스레드의 백그라운드 작업 구현
    //여기서 매개변수 Intger ... values란 values란 이름의 Integer배열이라 생각하면된다.
    //배열이라 여러개를 받을 수 도 있다. ex) excute(100, 10, 20, 30); 이런식으로 전달 받으면 된다.
    @Override
    protected Integer doInBackground(Void... voids) {
        return null;
    }

    //UI작업 관련 작업 (백그라운드 실행중 이 메소드를 통해 UI작업을 할 수 있다)
    //publishProgress(value)의 value를 값으로 받는다.values는 배열이라 여러개 받기가능
    @Override
    protected void onProgressUpdate(Integer ... values) {

    }


    //이 Task에서(즉 이 스레드에서) 수행되던 작업이 종료되었을 때 호출됨
    @Override
    protected void onPostExecute(Integer result) {

    }

    //Task가 취소되었을때 호출
    @Override
    protected void onCancelled() {

    }
}