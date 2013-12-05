#Assumes both starting table cells are selected
Settings.MoveMouseDelay = 0.1
spreadsheet = "Excel"

#Copy data over
rows = input("How many rows do you want to copy?")
cols = input("How many columns do you want to copy over?")
#For each column
for y in range(0,int(cols)):
    for x in range(0,int(rows)):
        switchApp(spreadsheet)
        type("c",KEY_CTRL)
        wait(0.1)
        type(Key.DOWN)
        switchApp("Netbeans")
        type(Key.F2)
        type("v", KEY_CTRL)
        type(Key.DOWN)
    #Go to next column in Netbeans☻
    for r in range(0, int(rows)):
        type(Key.UP)
    type(Key.RIGHT)
    #Go to next column in Calc
    switchApp(spreadsheet)
    for r in (range(0, int(rows))):
            type(Key.UP)
    type(Key.RIGHT)
    