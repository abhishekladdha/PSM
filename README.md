# PSM
Source Code and Data for *[Extracting Aspect Specific Opinion Expression](https://aclweb.org/anthology/D16-1060)* (EMNLP 2016)

## Usage

1. Import the directory into Eclipse (recommended).
2. Build the project
3. Run the program by launch.MainEntry

- The commandline arguments are stored in the file "global/CmdOption.java". If no argument is provided, the program uses the default arguments. There are several arguments that are subject to change:

  -i: the path of input domains directory in specified format <br />
  -o: the path of output model directory <br />
  -nTopics: the number of topics used in Topic Model for each domain 

We are providing the trained model of CRF . CRF model can be trained using following command with additional data.
```
% crf_learn template_file train_file model_file
```

## Input and Output File Format

**Input**

The input directory should contain domain folders. In each Domain folder there should be files for each aspect in the following format where each line has one word, every feature of word in line is tab separated and sentence are separated by one line.

WORD  HA  POS  CHUNK  PREFIX SUFFIC SENTIMENT LABEL

HA: Whether the wrod is Head Aspect or not
All other features are described in paper.
e.g.
```
It	N	PRP	B-NP	NULL	NULL	NULL	0
doesn't	N	RB	B-ADVP	NULL	NULL	NULL	0
track	N	VBP	B-VP	NULL	NULL	NULL	0
the	N	DT	B-NP	NULL	NULL	NULL	0
finger	N	NN	I-NP	NULL	ER	NULL	0
well	N	RB	B-ADVP	NULL	NULL	POS	0
at	N	IN	B-PP	NULL	NULL	NULL	0
all	N	PDT	B-NP	NULL	NULL	NULL	0
no	N	DT	I-NP	NULL	NULL	NULL	0
matter	N	NN	I-NP	NULL	ER	NULL	0
how	N	WRB	B-ADJP	NULL	NULL	NULL	0
deliberate	N	JJ	I-ADJP	DE	NULL	NULL	0
the	N	DT	B-NP	NULL	NULL	NULL	0
gesture	N	NN	I-NP	NULL	NULL	NULL	0
often	N	RB	B-VP	NULL	EN	NULL	0
spinning	N	VBG	I-VP	NULL	ED	NULL	0
the	N	DT	B-NP	NULL	NULL	NULL	0
screen	Y	NN	I-NP	NULL	EN	NULL	0
off	N	RP	I-NP	NULL	NULL	NULL	0
in	N	IN	B-PP	NOT	NULL	NULL	0
unpredictable	N	JJ	B-NP	UN	BLE	NEG	0
direction	Y	NN	I-NP	NULL	ION	NULL	0
.	N	.	O	NULL	NULL	NULL	0
```
**Output**

There will a list of domain folders in output directory where each domain folder contains topic model results for each domain. Under each domain folder, there are 5 files (can be opened by text editors):
- domain.twords: top words under each topic. The columns are separated by '\t' where each column corresponds to each topic.
- domain.docs: each line (representing a document) contains a list of word ids.
- domain.tassign: topic assignment for each word in each document.
- domain.twdist: topic-word distribution
- domain.vocab: mapping from word id (starting from 0) to word.

## Reference
Please cite the following paper if you found the codes/datasets useful:
```
@inproceedings{laddha2016extracting,
  title={Extracting Aspect Specific Opinion Expressions.},
  author={Laddha, Abhishek and Mukherjee, Arjun},
  booktitle={EMNLP},
  pages={627--637},
  year={2016}
}
```
