# README

This is the simple demo of the paper **"Where to Handle an Exception? Recommending Exception Handling Locations from a Global Perspective"**.

We release this demo to simply illustrate the preliminary use flow of EHAdvisor.

Given a project and a specific method, this demo will output the location recommendations to handle the exceptions thrown by this method on all its call chains.

## Usage

1. Obtain a base binary-classification model. You can use a pre-trained model we release on our webpage or train a model with `python trainModel.py` (please refer to the replication package instruction for the details about training).
2. Run `python tools.py` to execute the demo script.
3. Input the path to the source code of the target project after the script prompts "Please input the path of the source folder:", e.g., `shiro`.
4. Input the path to the description text of the target project after the script prompts "Please input the path of the project document file:", e.g., `shiro/readme.txt`.
5. Input the method requiring for the location recommendations after the scripts promts "Please input the name of method:", e.g., `org.apache.shiro.cache.ehcache.EhCacheManager$ensureCacheManager`.
6. Then, you will get the location recommendations to handle the exceptions thrown by this method on all its call chains.
