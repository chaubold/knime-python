import time
import streamlit as st
import seaborn as sns
import matplotlib.pyplot as plt

import knime_arrow_table as kat

import streamlit_data_holder


@st.experimental_singleton
def get_dataframe():
    # Wait for the data to be populated
    while True:
        if streamlit_data_holder.data != None:
            break
        time.sleep(1)

    # Convert the data to pandas
    with streamlit_data_holder.data as ds:
        table = kat.ArrowReadTable(ds)
        return table.to_pandas()


df = get_dataframe()

time.sleep(10)

# ------- The real data app starts here ----------

st.write("# THIS IS STREAMLIT")

if st.checkbox("Show table"):
    st.write(df)
else:
    fig = plt.figure(figsize=(10, 4))
    sns.scatterplot(data=df, x="Universe_0_0", y="Universe_0_1")
    st.pyplot(fig)
