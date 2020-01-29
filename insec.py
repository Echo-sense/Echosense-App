def intersects(line1, line2):
    def onSeg(p, q, r):
        if (q[0] <= max(p[0],r[0]) and q[0] >= min(p[0],r[0]) and
            q[0] <= max(p[1],r[1]) and q[1] >= min(p[1],r[1])):
            return True
        return False
    #0 -> colinear, 1 -> clockwise, 2 -> ccw 
    def orientation(p,q,r):
        val = (q[1]-p[1]) * (r[0] - q[0]) - (q[0]-p[0]) * (r[1] - q[1])
        if val == 0:
            return 0
        if val > 0:
            return 1
        return 2
    p1 , q1 = line1
    p2 , q2 = line2
    o1 = orientation(p1, q1, p2)
    o2 = orientation(p1, q1, q2)
    o3 = orientation(p2, q2, p1)
    o4 = orientation(p2, q2, q1)

    x1, y1 = p1
    x2, y2 = q1
    x3, y3 = p2
    x4, y4 = q2
    
    if (o1 != o2 and o3 != o4): #general
        xd = (x1*y2 - y1*x2)*(x3-x4)-(x1-x2)*(x3*y4-y3*x4)
        xn = (x1-x2)*(y3-y4) - (y1-y2)*(x3-x4)
        yd = (x1*y2 - y1*x2)*(y3-y4)-(y1-y2)*(x3*y4-y3*x4)
        yn = (x1-x2)*(y3-y4) - (y1-y2)*(x3-x4)
        return (xd/xn , yd/yn)
    if (o1 == 0 and onSeg(p1,p2,q1)): #p2 lies on p1q1
        return p2
    if (o2 == 0 and onSeg(p1,q2,q1)): #q2 lies on p1q1
        return q2
    if (o3 == 0 and onSeg(p2,p1,q2)): #p1 lies on p2q2
        return p1
    if (o4 == 0 and onSeg(p2,q1,q2)): #q1 lies on p2q2
        return q1
    return (-1,-1)
    
