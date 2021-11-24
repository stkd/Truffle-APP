# Truffle-APP
## 松露地底攝影機專用APP
### 內部功能
1. 與OTG相機設備連動功能
2. 拍照、錄影功能
3. 紀錄拍/錄時間、地點、其他資料
4. 調閱以往照片、錄影

#### APK Output place
turfflev2/build/outputs/apk/debug

### 使用方法
下面是選擇USB Camera的列表，如果有要使用，不用一定要開啟APP，只需要將地底攝影設備以OTG(就是USB連線，只是要支援OTG)連線，便會跳出通知以開啟APP，開啟後，如果沒有連到設備，則需要用左下角開關按鈕來手動選擇相機來開啟。
![](https://i.imgur.com/FdG0W75.png)
:::warning
1. 除了下面功能，v2版還有新增藍芽連線，目的是要用於控制鏡頭焦距，在上圖下面有顯示可填入的部分則是控制調整速度的快慢(0~255，速度有下限，盡量不要低於122)
2. 藍芽的部分請在連線上相機看到畫面後便執行連線，連線完後才能透過拍照按鈕左右兩邊的按鍵控制鏡頭焦距。
:::
以下是基本功能介紹(v1)
![](https://i.imgur.com/meh35th.png)

### 注意事項
由於有使用到c++，因此需要NDK支援

NDK版本為androidNDK-r13b(16 or 17)

https://dl.google.com/android/repository/android-ndk-r13b-windows-x86_64.zip?utm_source=androiddevtools&utm_medium=website

### 引用資料
1. [saki4510t/UVCCamera]https://github.com/saki4510t/UVCCamera
2. [jiangdongguo/AndroidUSBCamera]https://github.com/jiangdongguo/AndroidUSBCamera

