# create environments for base and mdf, searching for the knime-python metapackages in conda-bld
micromamba env create -n basedepsmamba knime-python-base python=3.9 -c /home/chaubold/miniconda3/conda-bld -c conda-forge --override-channels
micromamba env create -n mdfdepsmamba knime-python-mdf python=3.9 -c /home/chaubold/miniconda3/conda-bld -c conda-forge --override-channels

# get list of all required packages in those environments
micromamba activate basedepsmamba
conda list --explicit > basedepsmamba.txt # must be done with conda, micromamba doesn't have the "explicit" flag
micromamba activate mdfdepsmamba
conda list --explicit > mdfdepsmamba.txt

# Download all required packages to our own local "channel".
# A "linux-64" subfolder is needed because conda sorts packages by architecture like this
# (needs python and requests in the active env)
python /home/chaubold/src/knime-python/org.knime.python3.conda.recipes/get_packages.py basedepsmamba.txt --dest base_channel/linux-64
python /home/chaubold/src/knime-python/org.knime.python3.conda.recipes/get_packages.py mdfdepsmamba.txt --base basedepsmamba.txt --dest mdf_channel/linux-64
# create package repo index
conda index base_channel
conda index mdf_channel

# create envs from the local channels
micromamba create -n basechanneltest -c ./base_channel --override-channels knime-python-base
micromamba create -n mdfchanneltest -c ./base_channel -c ./mdf_channel --override-channels knime-python-mdf
# these also work
# conda create -n basechanneltest -c ./base_channel --override-channels knime-python-base
# conda create -n mdfchanneltest -c ./base_channel -c ./mdf_channel --override-channels knime-python-mdf

# cleaning up
micromamba remove -n basedepsmamba -a -y
micromamba remove -n mdfdepsmamba -a -y
micromamba clean -a -y # -y always answers yes
