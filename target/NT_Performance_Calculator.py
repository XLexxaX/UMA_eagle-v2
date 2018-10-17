
# coding: utf-8

# In[9]:

import re
import os

def calc_performance(goldf, predf):
    relationMatcher = re.compile('.*<.*>.*<.*>.*<.*>.*')

    gold = dict()
    pred = dict()
    goldfile = open(goldf, "r")
    predfile = open(predf, "r")
    #predfile = open("C:/Users/Alexander/Desktop/data_phones/pipetest.nt", "r")
    for line in goldfile:
        if relationMatcher.match(line):
            if line.split('<')[1].split('>')[0] in list(gold.keys()):
                gold[line.split('<')[1].split('>')[0]] = gold[line.split('<')[1].split('>')[0]] + [line.split('<')[3].split('>')[0]]
            else:
                gold[line.split('<')[1].split('>')[0]] = [line.split('<')[3].split('>')[0]]

    for line in predfile:
        if relationMatcher.match(line):
            if line.split('<')[1].split('>')[0] in list(pred.keys()):
                pred[line.split('<')[1].split('>')[0]] = pred[line.split('<')[1].split('>')[0]] + [line.split('<')[3].split('>')[0]]
            else:
                pred[line.split('<')[1].split('>')[0]] = [line.split('<')[3].split('>')[0]]


    tp=0
    fp=0
    for k,v in pred.items():
        for i in v:
            found = False
            if k in list(gold.keys()):
                if (i in gold[k]):
                    found = True
            elif i in list(gold.keys()):
                if (k in gold[i]):
                    found = True
            if found:
                tp = tp + 1
            else:
                fp = fp + 1

    tp=0
    fn=0
    for k,v in gold.items():
        for i in v:
            found = False
            if k in list(pred.keys()):
                if (i in pred[k]):
                    found = True
            elif i in list(pred.keys()):
                if (k in pred[i]):
                    found = True
            if found:
                tp = tp + 1
            else:
                fn = fn + 1

    r=0
    p=0
    f=0
    try:
        r = tp / (tp + fn)*100
    except:
        r = 0
    try:
        p = tp / (tp + fp)*100
    except:
        p = 0
    try:
        f= 2*p*r/(p+r)
    except:
        f = 0
    print("\n-> Final F1-Score: ", f,"% (r:", r,"%, p:", p,"%)", sep="")


# In[ ]:

#calc_performance("C:/Users/Alexander/Desktop/data_phones/alignment.nt","B:/Development/limes3/limes/limes-core/target/acceptance.nt")
#calc_performance("B:/Development/limes2/limes/limes-core/src/main/resources/datasets/dailymed-drugbank-ingredients/reference2.nt","B:/Development/limes3/limes/limes-core/target/dailymed_drugbank_incredients_accepted.txt")
calc_performance("B:/Development/limes2/limes/limes-core/src/main/resources/datasets/dbpedia-linkedmdb/reference2.nt","B:/Development/limes3/limes/limes-core/target/dbpedia-linkedmdb_accepted.nt")

# In[ ]:

if __name__=="__main__":

    from optparse import OptionParser
    optparser = OptionParser(description="Predict events/relations")
    optparser.add_option("-g", "--gold", default=None, dest="input", help="input")
    optparser.add_option("-p", "--predicted", default=None, dest="output", help="output file stem")
    (options, args) = optparser.parse_args()

    #assert options.gold != None
    #assert options.predicted != None
    #if not options.gold == None and not options.predicted == None:
    #    calc_performance(options.gold, options.predicted)


# In[ ]:




# In[ ]:
