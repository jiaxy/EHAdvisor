
for i in {0..28}
 do
 	python across.py --train_id=$i
 	python top.py
 	python deleteKXM.py
 	python intraplus.py --train_id=$i
 	python top.py
 	python deleteKXM.py
 done


