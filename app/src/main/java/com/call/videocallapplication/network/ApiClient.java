package com.call.videocallapplication.network;

import com.call.videocallapplication.utils.Constants;

import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

/*
 * This Class Using Singleton Design Pattern
 * (Nếu bạn chưa rỏ về mô hình này thì bạn có thể đọc tài liệu trước khi tham khảo)
 */
public class ApiClient {
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(Constants.BASE_URL_API)
                    .addConverterFactory(ScalarsConverterFactory.create()) // Can use GSON or SCALARS
                    .build();
        }
        return retrofit;
    }
}
