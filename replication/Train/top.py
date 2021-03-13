import numpy as np
import pandas as pd
import os
throwThreshold=0.5

gradient=0
delThrow=False
delThrowOrOneCatch =False
delOneLength = False
delTwoLength = False
filePath ="../dataset/resultsKXM"
list=[]
def ReadFile(filePath):
    files = os.listdir(filePath)

    for file in files:
        list1 = []
        file=open(filePath+"//"+file)
        for line in file:
            list.append(line)
        # print(file.name)
        # global delThrow
        # delThrow= True
        # printTop(list1)
        # delThrow = False
    return list
def SplitListOne(list):
    splitList=[]
    targetList=[]
    for line in list:
        splitLine = line.split('::')

        targetList.append([splitLine[0],splitLine[2]])

        sList = []
        for i in range(len(splitLine)-3):
            sList.append(splitLine[i+3])
        splitList.append(sList)
    return targetList,splitList
def SplitListTwo(list):
    splitList=[]
    for line in list:
        lineList = []
        splitLine = line[0].split("->")
        splitLine.remove('\n')
        for i in range(len(splitLine)):
            splitline = splitLine[i].split("|")
            lineList.append(splitline)
        splitList.append(lineList)
    return splitList
def HandleTarget(targetlist):
    for i in range(len(targetlist)):
        if targetlist[i][1]=='0':
            targetlist[i][0]='throw'
    return targetlist
def DelThrowAndOneCatch(targetList,splitList):
    listLength = len(targetList)
    for i in range(listLength):
        if targetList[listLength-i-1][0]=="throw":
            del(targetList[listLength-i-1])
            del(splitList[listLength-i-1])
    listLength = len(targetList)
    for i in range(listLength):
        if len(splitList[listLength-i-1])<2:
            del(targetList[listLength-i-1])
            del(splitList[listLength-i-1])
    return targetList,splitList
def DelOneLength(targetList,splitList):
    onelengthnum=0
    listLength = len(targetList)
    for i in range(listLength):
        if len(splitList[listLength-i-1])<2:
            onelengthnum=onelengthnum+1
            del(targetList[listLength-i-1])
            del(splitList[listLength-i-1])
    return targetList,splitList,onelengthnum
def DelTwoLength(targetList,splitList):
    twolengthnum = 0
    listLength = len(targetList)
    for i in range(listLength):
        if len(splitList[listLength-i-1])<3:
            twolengthnum=twolengthnum+1
            del(targetList[listLength-i-1])
            del(splitList[listLength-i-1])
    return targetList,splitList,twolengthnum
def DelThrow(targetList,splitList):
    listLength = len(targetList)
    for i in range(listLength):
        if targetList[listLength-i-1][0]=="throw":
            del(targetList[listLength-i-1])
            del(splitList[listLength-i-1])
    return targetList,splitList
def GetRightPosition(target,split):
    for i in range(len(split)):
        if target[0]==split[i][0]:
            return i+1
    return 0
def GetRightPositon(targetList,splitList):
    twoLenCatch=[]
    threeLenCatch=[]
    fourLenCatch=[]
    fiveLenCatch=[]
    sixLenCatch = []
    sevenLenCatch=[]
    for i in range(len(targetList)):
        if len(splitList[i])==2:
            twoLenCatch.append(GetRightPosition(targetList[i],splitList[i]))
        elif len(splitList[i])==3:
            threeLenCatch.append(GetRightPosition(targetList[i], splitList[i]))
        elif len(splitList[i])==4:
            fourLenCatch.append(GetRightPosition(targetList[i], splitList[i]))
        elif len(splitList[i])==5:
            fiveLenCatch.append(GetRightPosition(targetList[i], splitList[i]))
        elif len(splitList[i])==6:
            sixLenCatch.append(GetRightPosition(targetList[i], splitList[i]))
        elif len(splitList[i])==7:
            sevenLenCatch.append(GetRightPosition(targetList[i], splitList[i]))
    DrawGraph(twoLenCatch,2)
    DrawGraph(threeLenCatch, 3)
    DrawGraph(fourLenCatch, 4)
    DrawGraph(fiveLenCatch, 5)
    DrawGraph(sixLenCatch, 6)
    DrawGraph(sevenLenCatch, 7)

    return 0
def DrawGraph(list,length):
    x=[]
    y=[]
    for i in range(length):
        x.append(i+1)
        y.append(0)
    for i in range(len(list)):
        if list[i]-1==-1:
            continue
        y[list[i]-1]+=1
    for i in range(length):
        y[i]=y[i]/len(list)
    import matplotlib as mpl
    import matplotlib.pyplot as plt
    plt.scatter(x,y)
    plt.show()
def Rec(list):
    recList=[]
    for line in list:
        recList.append(GetRec(line))
    return recList
def printTop(list):
    #list = ReadFile(filePath)


    targetList, splitList = SplitListOne(list)
    targetList = HandleTarget(targetList)
    splitList = SplitListTwo(splitList)



    onelength=0
    twolength=0
    if delThrowOrOneCatch==True:
        targetList,splitList=DelThrowAndOneCatch(targetList,splitList)

    if delOneLength==True:
        targetList,splitList,onelength=DelOneLength(targetList,splitList)

    if delTwoLength==True:
        targetList,splitList,twolength=DelTwoLength(targetList,splitList)

    if delThrow ==True:
        targetList,splitList=DelThrow(targetList,splitList)

    recList = Rec(splitList)

    top,throwNum = Compare(targetList,recList)

    top[1]+=top[0]
    top[2]+=top[1]
    #print(twolength)
    print("sumNum：", len(recList))
    print("top1：",top[0],"top2：",top[1],"top3：",top[2])

    print(" &{:10.4f}".format(top[0]/(len(recList)))," &{:10.4f}".format(top[1]/(len(recList)))," &{:10.4f}".format(top[2]/(len(recList))))
def GetRec(line):
    recList=[]
    for i in range(len(line)):
        recList.append(line[i])
    recList=Sort(recList)
    recList=InsertThrow(recList)
    return recList
def Sort(list):
    n=len(list)

    # if(n!=1):
    #     list[n-1][1]=str(float(list[n-1][1])-1)

    for i in range(n):
        list[i][1] = str(float(list[i][1]) - (i+1)*gradient)
    for i in range(n):
        # Last i elements are already in place
        for j in range(0, n - i - 1):
            if float(list[j][1]) < float(list[j + 1][1]):
                list[j], list[j + 1] = list[j + 1], list[j]
    return list
def Compare(targetList,recList):
    top=[0,0,0,0]
    throwNum=0
    for i in range(len(targetList)):
        if targetList[i][0]=="throw":
            throwNum+=1
        #     continue;

        for j in range(len(recList[i])):
            if j>2:
                break;
            if targetList[i][0]==recList[i][j][0]:
                top[j]+=1
                break;
    return top,throwNum


def InsertThrow(list):
    for i in range(len(list)):
        if throwThreshold>float(list[i][1]):
            list.insert(i,["throw",0])
            return list
    list.append(["throw",0])
    return list
def PLengthOflist(list):
    length=[0,0,0]
    for i in range(len(list)):
        if len(list[i])==1:
            length[0]+=1
        elif len(list[i])==2:
            length[1]+=1
        elif len(list[i]) == 3:
            length[2] += 1
    print("len1",length[0],"len2",length[1],"len3",length[2])

if __name__ == '__main__':

    ReadFile(filePath)
    printTop(list)