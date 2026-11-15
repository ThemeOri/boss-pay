# কীভাবে APK পাবেন

এই স্যান্ডবক্সে Google-এর Android SDK ব্লক থাকায় এখানে APK বানানো যায়নি।
কোড না ছুঁয়ে APK পাওয়ার দুটো সহজ পথ:

## পথ ১ — GitHub-এ অটো বিল্ড (কম্পিউটারে কিছু ইনস্টল লাগে না)
1. github.com-এ একটা ফ্রি অ্যাকাউন্ট/রিপো বানান (New repository)।
2. এই ফোল্ডারের সব ফাইল সেই রিপোতে আপলোড করুন (Add file → Upload files → টেনে ছেড়ে দিন)।
   `.github` ফোল্ডারটাও যেন যায়, খেয়াল রাখবেন।
3. আপলোড শেষ হলে রিপোর **Actions** ট্যাবে যান। "Build APK" নিজে থেকে চলবে
   (না চললে বাঁ পাশে Build APK → Run workflow)।
4. সবুজ টিক হলে সেই রান খুলুন → নিচে **Artifacts → app-debug-apk** নামায় APK পাবেন।
5. ফোনে কপি করে ইনস্টল করুন (Settings-এ "Unknown sources"/"এই উৎস থেকে ইনস্টল" চালু রাখতে হবে)।

## পথ ২ — Android Studio (এক ক্লিক, কোড লিখতে হয় না)
1. Android Studio খুলে এই ফোল্ডারটা Open করুন, একটু অপেক্ষা করুন (Gradle sync)।
2. উপরের মেনু **Build → Build Bundle(s)/APK(s) → Build APK(s)**।
3. নিচে "locate" লিংক থেকে `app/build/outputs/apk/debug/app-debug.apk` পাবেন।

APK ইনস্টলের পর অ্যাপে সার্ভার URL, Pair কোড দিন (App Key আগে থেকেই বসানো),
"চালু" টিক দিয়ে SMS অনুমতি দিন।
